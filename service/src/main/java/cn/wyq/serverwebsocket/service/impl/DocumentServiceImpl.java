package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.exception.ServiceException;
import cn.wyq.serverwebsocket.mapper.SysDocumentMapper;
import cn.wyq.serverwebsocket.pojo.entity.SysDocument;
import cn.wyq.serverwebsocket.service.DocumentService;
import cn.wyq.serverwebsocket.service.SysConfigService;
import cn.wyq.serverwebsocket.utils.CacheClient;
import cn.wyq.serverwebsocket.utils.SystemConfigUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * DocumentServiceImpl：业务服务实现类，封装对应领域的业务规则。
 */
@Service
public class DocumentServiceImpl extends ServiceImpl<SysDocumentMapper, SysDocument> implements DocumentService {

    private static final String VECTOR_CACHE_PATH = "web/src/main/resources/static/dataset/mecha_vector_cache.json";
    private static final String DOCUMENT_LIST_CACHE_PREFIX = "documents:user";
    private static final String DEFAULT_PRIVATE_DOCUMENT_DIRECTORY = "private-ai-documents";
    private static final Pattern PRIVATE_STORAGE_KEY_PATTERN = Pattern.compile("^[a-f0-9]{32}\\.(?:pdf|txt|md)$");
    private static final String CONTENT_HASH_UNIQUE_INDEX = "uk_sys_document_owner_hash";
    private static final HexFormat HEX_FORMAT = HexFormat.of();

    @Autowired
    private VectorStore vectorStore;
    @Autowired
    private CacheClient cacheClient;
    @Autowired
    private SysConfigService sysConfigService;

    /**
     * AI 文档专用的私有目录。留空时使用应用工作目录下的 private-ai-documents，
     * 与 image.location 对应的 /file/** 公共目录完全隔离。
     */
    @Value("${ai.document.storage.location:}")
    private String privateDocumentStorageLocation;

    @Value("${image.location:}")
    private String publicUploadLocation;

    /** 单个文档的原始文件大小上限，避免解析器处理不受控的大文件。 */
    @Value("${ai.document.limits.max-file-size-bytes:10485760}")
    private long maxDocumentFileSizeBytes;

    /** PDF 页数上限，在创建 Spring AI PDF 读取器前先做轻量预检。 */
    @Value("${ai.document.limits.max-pdf-pages:100}")
    private int maxPdfPages;

    /** 解析出的原始文本总字符上限，避免压缩 PDF 或文本文件放大内存占用。 */
    @Value("${ai.document.limits.max-extracted-characters:200000}")
    private int maxExtractedCharacters;

    /** 单个文档允许写入向量库的最大切片数。 */
    @Value("${ai.document.limits.max-slices:400}")
    private int maxDocumentSlices;

    /** 本次上传采用的动态文档限制；配置表不可用时回退到 application.yaml 默认值。 */
    private record DocumentLimits(long maxUploadBytes, int chunkSize, int chunkOverlap,
                                 int maxSlices, Set<String> allowedExtensions) {
    }

    @Override
    public List<SysDocument> listByOwner(Integer ownerUserId) {
        requireOwnerUserId(ownerUserId);
        return cacheClient.queryWithMutex(DOCUMENT_LIST_CACHE_PREFIX, ownerUserId,
                new TypeReference<List<SysDocument>>() {}, ignored -> {
                    List<SysDocument> documents = super.list(Wrappers.<SysDocument>lambdaQuery()
                            .eq(SysDocument::getOwnerUserId, ownerUserId)
                            .orderByDesc(SysDocument::getCreateTime));
                    return documents == null ? List.of() : documents;
                }, 5, java.util.concurrent.TimeUnit.MINUTES);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadAndParse(MultipartFile file, Integer ownerUserId) {
        requireOwnerUserId(ownerUserId);
        DocumentLimits limits = resolveDocumentLimits();
        validateDocumentLimits(limits);
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空", 400);
        }
        validateFileSize(file, limits.maxUploadBytes());

        String originalFilename = normalizeOriginalFilename(file.getOriginalFilename());
        String fileExtension = extractAllowedExtension(originalFilename, limits.allowedExtensions());
        if (fileExtension == null) {
            throw new ServiceException("仅支持解析 PDF、TXT 和 Markdown 格式的文件", 400);
        }
        String contentHash = calculateContentHash(file, limits.maxUploadBytes());
        if (this.exists(Wrappers.<SysDocument>lambdaQuery()
                .eq(SysDocument::getOwnerUserId, ownerUserId)
                .eq(SysDocument::getContentHash, contentHash))) {
            throw new ServiceException("该文档内容已上传，请勿重复上传", 409);
        }

        // 私有文件仅使用随机存储键，原始文件名绝不参与服务器路径拼接。
        String docId = UUID.randomUUID().toString().replace("-", "");
        String storageKey = docId + fileExtension;
        Path storedFile = null;

        try {
            // 文件只保存到不受 /file/** 静态映射覆盖的私有根目录。
            storedFile = resolvePrivateStorageFile(storageKey);

            // 先用唯一约束预留文档记录。并发上传相同内容时，后续请求会在这里被数据库拒绝，
            // 不会继续写私有文件、解析内容或重复调用向量库。
            SysDocument sysDocument = new SysDocument();
            sysDocument.setId(docId);
            sysDocument.setOwnerUserId(ownerUserId);
            sysDocument.setFileName(originalFilename);
            sysDocument.setFilePath(storageKey);
            sysDocument.setFileSize(file.getSize());
            sysDocument.setContentHash(contentHash);
            if (!this.save(sysDocument)) {
                throw new ServiceException("保存文档信息失败", 500);
            }

            file.transferTo(storedFile.toFile());

            // 直接读取私有物理文件进行解析，不暴露任何下载 URL 给向量元数据。
            Resource resource = new FileSystemResource(storedFile);
            List<Document> documents;

            if (".pdf".equals(fileExtension)) {
                validatePdfPageCount(storedFile);
                PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
                documents = pdfReader.get();
            } else {
                TextReader textReader = new TextReader(resource);
                textReader.getCustomMetadata().put("fileType", fileExtension);
                documents = textReader.get();
            }

            validateExtractedDocuments(documents);

            // 6. 文本切片
            TokenTextSplitter textSplitter = new TokenTextSplitter(
                    limits.chunkSize(),
                    Math.max(1, Math.min(350, limits.chunkSize() - 1)),
                    5,
                    10000,
                    true
            );
            List<Document> splitDocuments = textSplitter.apply(documents);
            splitDocuments = applyChunkOverlap(splitDocuments, limits.chunkOverlap());
            validateSplitDocuments(splitDocuments, limits.maxSlices());

            // 7. 注入 Metadata
            for (Document doc : splitDocuments) {
                doc.getMetadata().put("docId", docId);
                doc.getMetadata().put("ownerUserId", ownerUserId);
                doc.getMetadata().put("fileName", originalFilename);
                doc.getMetadata().put("knowledgeBase", "document");
            }

            // 8. 写入向量库，并持久化缓存，避免服务重启后丢失上传文档的向量
            vectorStore.accept(splitDocuments);
            persistVectorStore();
            evictDocumentListAfterCommit(ownerUserId);

            return docId;

        } catch (Exception e) {
            deleteStoredFileAfterFailedUpload(storedFile);
            if (e instanceof ServiceException serviceException) {
                throw serviceException;
            }
            if (isDuplicateContentHashViolation(e)) {
                throw new ServiceException("该文档内容已上传，请勿重复上传", 409);
            }
            throw new ServiceException("文档上传与解析失败: " + e.getMessage(), 500);
        }
    }

    private DocumentLimits resolveDocumentLimits() {
        long fallbackUploadBytes = maxDocumentFileSizeBytes;
        int fallbackChunkSize = 800;
        int fallbackChunkOverlap = 120;
        int fallbackMaxSlices = maxDocumentSlices;
        try {
            Map<String, String> values = sysConfigService.getConfigMap();
            long maxUploadBytes = SystemConfigUtils.longValue(values, "rag_max_upload_bytes",
                    fallbackUploadBytes, 1_024L, 1_073_741_824L);
            int chunkSize = SystemConfigUtils.intValue(values, "rag_chunk_size",
                    fallbackChunkSize, 100, 10_000);
            int chunkOverlap = SystemConfigUtils.intValue(values, "rag_chunk_overlap",
                    fallbackChunkOverlap, 0, 2_000);
            int maxSlices = SystemConfigUtils.intValue(values, "rag_max_slices",
                    fallbackMaxSlices, 1, 10_000);
            return new DocumentLimits(maxUploadBytes, chunkSize,
                    Math.min(chunkOverlap, Math.max(0, chunkSize - 1)), maxSlices,
                    parseAllowedExtensions(values.get("rag_allowed_file_types")));
        } catch (RuntimeException exception) {
            return new DocumentLimits(fallbackUploadBytes, fallbackChunkSize,
                    Math.min(fallbackChunkOverlap, fallbackChunkSize - 1), fallbackMaxSlices,
                    defaultAllowedExtensions());
        }
    }

    private void validateDocumentLimits(DocumentLimits limits) {
        if (limits == null || limits.maxUploadBytes() <= 0 || limits.chunkSize() <= 0
                || limits.chunkOverlap() < 0 || limits.chunkOverlap() >= limits.chunkSize()
                || limits.allowedExtensions() == null || limits.allowedExtensions().isEmpty()
                || maxPdfPages <= 0 || maxExtractedCharacters <= 0 || limits.maxSlices() <= 0) {
            throw new ServiceException("AI 文档限制配置无效", 500);
        }
    }

    private void validateFileSize(MultipartFile file, long maxUploadBytes) {
        if (file.getSize() > maxUploadBytes) {
            throw new ServiceException("上传文件大小超出限制，最大允许 "
                    + maxUploadBytes / (1024 * 1024) + "MB", 400);
        }
    }

    private String calculateContentHash(MultipartFile file, long maxUploadBytes) {
        try (InputStream inputStream = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            long bytesRead = 0;
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                bytesRead += read;
                if (bytesRead > maxUploadBytes) {
                    throw new ServiceException("上传文件大小超出限制，最大允许 "
                            + maxUploadBytes / (1024 * 1024) + "MB", 400);
                }
                digest.update(buffer, 0, read);
            }
            return HEX_FORMAT.formatHex(digest.digest());
        } catch (IOException e) {
            throw new ServiceException("读取上传文件失败", 400);
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceException("无法初始化文档内容校验", 500);
        }
    }

    private void validatePdfPageCount(Path storedFile) {
        try (PDDocument pdfDocument = Loader.loadPDF(storedFile.toFile())) {
            if (pdfDocument.isEncrypted()) {
                throw new ServiceException("不支持解析加密 PDF 文档", 400);
            }
            int pageCount = pdfDocument.getNumberOfPages();
            if (pageCount == 0) {
                throw new ServiceException("PDF 文档未包含可解析页面", 422);
            }
            if (pageCount > maxPdfPages) {
                throw new ServiceException("PDF 页数超出限制，最多允许 " + maxPdfPages + " 页", 400);
            }
        } catch (IOException e) {
            throw new ServiceException("PDF 文档无法解析", 400);
        }
    }

    private void validateExtractedDocuments(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            throw new ServiceException("文档未提取到可用文本", 422);
        }

        long extractedCharacters = 0;
        for (Document document : documents) {
            if (document == null || !StringUtils.hasText(document.getText())) {
                continue;
            }
            extractedCharacters += document.getText().length();
            if (extractedCharacters > maxExtractedCharacters) {
                throw new ServiceException("文档提取文本过长，最多允许 "
                        + maxExtractedCharacters + " 个字符", 400);
            }
        }
        if (extractedCharacters == 0) {
            throw new ServiceException("文档未提取到可用文本", 422);
        }
    }

    private void validateSplitDocuments(List<Document> splitDocuments, int maxSlices) {
        if (splitDocuments == null || splitDocuments.isEmpty()) {
            throw new ServiceException("文档未生成可用向量切片", 422);
        }
        if (splitDocuments.size() > maxSlices) {
            throw new ServiceException("文档切片数量超出限制，最多允许 "
                    + maxSlices + " 个切片", 400);
        }
    }

    /**
     * Spring AI M8 的 TokenTextSplitter 没有 overlap 参数；这里在切片结果上补齐上一片的尾部，
     * 使数据库配置的重叠长度真正影响向量上下文，同时复制元数据避免污染原文档对象。
     */
    private List<Document> applyChunkOverlap(List<Document> splitDocuments, int overlap) {
        if (overlap <= 0 || splitDocuments == null || splitDocuments.size() < 2) {
            return splitDocuments;
        }
        List<Document> overlapped = new ArrayList<>(splitDocuments.size());
        String previousText = null;
        for (Document document : splitDocuments) {
            if (document == null || !StringUtils.hasText(document.getText())) {
                continue;
            }
            String text = document.getText();
            if (previousText != null) {
                int start = Math.max(0, previousText.length() - Math.min(overlap, previousText.length()));
                text = previousText.substring(start) + "\n" + text;
            }
            Map<String, Object> metadata = document.getMetadata() == null
                    ? new HashMap<>() : new HashMap<>(document.getMetadata());
            Document overlappedDocument = new Document(text, metadata);
            overlappedDocument.setContentFormatter(document.getContentFormatter());
            overlapped.add(overlappedDocument);
            previousText = document.getText();
        }
        return overlapped;
    }

    private boolean isDuplicateContentHashViolation(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof DuplicateKeyException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.contains(CONTENT_HASH_UNIQUE_INDEX)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void persistVectorStore() {
        if (!(vectorStore instanceof org.springframework.ai.vectorstore.SimpleVectorStore simpleVectorStore)) {
            return;
        }
        File cacheFile = new File(VECTOR_CACHE_PATH);
        File parent = cacheFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("无法创建向量缓存目录: " + parent.getAbsolutePath());
        }
        simpleVectorStore.save(cacheFile);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(String docId, Integer ownerUserId) {
        requireOwnerUserId(ownerUserId);
        if (docId == null || docId.isBlank()) {
            throw new ServiceException("文档编号不能为空", 400);
        }

        SysDocument doc = this.getOne(Wrappers.<SysDocument>lambdaQuery()
                .eq(SysDocument::getId, docId)
                .eq(SysDocument::getOwnerUserId, ownerUserId));
        if (doc == null) {
            // 不区分不存在和他人文档，避免通过接口枚举文档编号。
            throw new ServiceException("文档不存在或无权访问", 404);
        }

        Path targetFile = resolvePrivateStorageFile(doc.getFilePath());

        boolean removed = this.remove(Wrappers.<SysDocument>lambdaQuery()
                .eq(SysDocument::getId, docId)
                .eq(SysDocument::getOwnerUserId, ownerUserId));
        if (!removed) {
            throw new ServiceException("删除文档失败", 500);
        }
        evictDocumentListAfterCommit(ownerUserId);

        try {
            Files.deleteIfExists(targetFile);
        } catch (IOException e) {
            throw new ServiceException("删除文档物理文件失败", 500);
        }

        // 向量库中可能保留历史切片，但后续会话绑定和检索都会校验文档归属，不能再被访问。
    }

    @Override
    public DownloadableDocument loadOwnedDocument(String docId, Integer ownerUserId) {
        requireOwnerUserId(ownerUserId);
        if (!StringUtils.hasText(docId)) {
            throw new ServiceException("文档编号不能为空", 400);
        }

        SysDocument document = this.getOne(Wrappers.<SysDocument>lambdaQuery()
                .eq(SysDocument::getId, docId)
                .eq(SysDocument::getOwnerUserId, ownerUserId));
        if (document == null) {
            throw new ServiceException("文档不存在或无权访问", 404);
        }

        Path storedFile = resolvePrivateStorageFile(document.getFilePath());
        if (!Files.isRegularFile(storedFile, LinkOption.NOFOLLOW_LINKS)) {
            throw new ServiceException("文档物理文件不存在", 404);
        }
        try {
            return new DownloadableDocument(new FileSystemResource(storedFile),
                    normalizeOriginalFilename(document.getFileName()), Files.size(storedFile));
        } catch (IOException e) {
            throw new ServiceException("读取文档失败", 500);
        }
    }

    private String extractAllowedExtension(String originalFilename, Set<String> allowedExtensions) {
        String fileExtension = extractFileExtension(originalFilename);
        if (allowedExtensions != null && allowedExtensions.contains(fileExtension)) {
            return fileExtension;
        }
        return null;
    }

    /** 当前解析器支持的文件类型；不把数据库配置中的未实现格式误当成可解析格式。 */
    private Set<String> parseAllowedExtensions(String configured) {
        if (!StringUtils.hasText(configured)) {
            return defaultAllowedExtensions();
        }
        Set<String> extensions = new HashSet<>();
        for (String item : configured.split(",")) {
            String normalized = item == null ? "" : item.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                if (!normalized.startsWith(".")) {
                    normalized = "." + normalized;
                }
                if (Set.of(".pdf", ".txt", ".md").contains(normalized)) {
                    extensions.add(normalized);
                }
            }
        }
        return extensions.isEmpty() ? defaultAllowedExtensions() : Set.copyOf(extensions);
    }

    private Set<String> defaultAllowedExtensions() {
        return Set.of(".pdf", ".txt", ".md");
    }

    private String extractFileExtension(String originalFilename) {
        if (originalFilename == null) {
            return null;
        }
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex <= 0 || lastDotIndex == originalFilename.length() - 1) {
            return null;
        }
        return originalFilename.substring(lastDotIndex).toLowerCase(Locale.ROOT);
    }

    private String normalizeOriginalFilename(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            throw new ServiceException("文件名不能为空", 400);
        }
        String normalized = originalFilename.replace('\\', '/');
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (!StringUtils.hasText(normalized)
                || normalized.indexOf('\u0000') >= 0
                || normalized.contains("\r")
                || normalized.contains("\n")) {
            throw new ServiceException("文件名不合法", 400);
        }
        return normalized;
    }

    private Path resolvePrivateStorageFile(String storageKey) {
        if (!StringUtils.hasText(storageKey) || !PRIVATE_STORAGE_KEY_PATTERN.matcher(storageKey).matches()) {
            throw new ServiceException("文档私有存储记录无效", 422);
        }
        Path privateRoot = privateStorageRoot();
        Path resolved = privateRoot.resolve(storageKey).normalize();
        if (!resolved.startsWith(privateRoot) || !resolved.getParent().equals(privateRoot)) {
            throw new ServiceException("文档私有存储记录无效", 422);
        }
        return resolved;
    }

    private Path privateStorageRoot() {
        try {
            Path configuredRoot = StringUtils.hasText(privateDocumentStorageLocation)
                    ? Paths.get(privateDocumentStorageLocation)
                    : Paths.get(System.getProperty("user.dir"), DEFAULT_PRIVATE_DOCUMENT_DIRECTORY);
            Path privateRoot = configuredRoot.toAbsolutePath().normalize();
            ensurePrivateRootIsNotPublic(privateRoot);
            Files.createDirectories(privateRoot);
            Path realPrivateRoot = privateRoot.toRealPath();
            ensurePrivateRootIsNotPublic(realPrivateRoot);
            return realPrivateRoot;
        } catch (InvalidPathException e) {
            throw new ServiceException("AI 文档私有存储目录配置无效", 500);
        } catch (IOException e) {
            throw new ServiceException("无法初始化 AI 文档私有存储目录", 500);
        }
    }

    private void ensurePrivateRootIsNotPublic(Path privateRoot) throws IOException {
        if (!StringUtils.hasText(publicUploadLocation)) {
            return;
        }
        Path publicRoot = Paths.get(publicUploadLocation).toAbsolutePath().normalize();
        if (hasPathOverlap(privateRoot, publicRoot)) {
            throw new ServiceException("AI 文档私有存储目录不能位于 /file/** 公共目录中", 500);
        }
        if (Files.exists(publicRoot) && Files.exists(privateRoot)) {
            Path realPublicRoot = publicRoot.toRealPath();
            Path realPrivateRoot = privateRoot.toRealPath();
            if (hasPathOverlap(realPrivateRoot, realPublicRoot)) {
                throw new ServiceException("AI 文档私有存储目录不能位于 /file/** 公共目录中", 500);
            }
        }
    }

    private boolean hasPathOverlap(Path firstPath, Path secondPath) {
        return firstPath.equals(secondPath)
                || firstPath.startsWith(secondPath)
                || secondPath.startsWith(firstPath);
    }

    private void deleteStoredFileAfterFailedUpload(Path storedFile) {
        if (storedFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(storedFile);
        } catch (IOException cleanupException) {
            log.warn("清理上传失败的私有文档文件失败: " + storedFile
                    + ", 原因: " + cleanupException.getMessage());
        }
    }

    private void requireOwnerUserId(Integer ownerUserId) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new ServiceException("用户身份无效", 401);
        }
    }

    private void evictDocumentListAfterCommit(Integer ownerUserId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            cacheClient.evict(DOCUMENT_LIST_CACHE_PREFIX, ownerUserId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cacheClient.evict(DOCUMENT_LIST_CACHE_PREFIX, ownerUserId);
            }
        });
    }
}
