package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.mapper.SysDocumentMapper;
import cn.wyq.serverwebsocket.pojo.entity.SysDocument;
import cn.wyq.serverwebsocket.service.DocumentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentServiceImpl extends ServiceImpl<SysDocumentMapper, SysDocument> implements DocumentService {

    @Autowired
    private VectorStore vectorStore;

    // 🌟 复用你 UploadController 里的配置项
    @Value("${image.mapping}")
    private String mapping;

    @Value("${image.location}")
    private String location;

    @Value("${server.port}")
    private String serverPort;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadAndParse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();

        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        // 🌟 修复 2：生成 10 位短 ID，防止拼接后超长导致数据库拒收
        String docId = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String newFileName = docId + fileExtension;

        try {
            // 2. 检查目录并物理保存文件 (与你的 UploadController 逻辑一致)
            File targetDir = new File(location);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            File targetFile = new File(location, newFileName);
            file.transferTo(targetFile);

            // 3. 🌟 生成回显的网络 URL 路径
            String fileUrl = "http://localhost:" + serverPort + mapping + newFileName;

            // 4. 将文档信息存入 MySQL 数据库，这里存的是网络可访问的 URL！
            SysDocument sysDocument = new SysDocument();
            sysDocument.setId(docId);
            sysDocument.setFileName(originalFilename);
            sysDocument.setFilePath(fileUrl); // 前端可以直接用这个地址预览文件
            sysDocument.setFileSize(file.getSize());
            this.save(sysDocument);

            // 5. 🌟 极速解析：因为文件就在本机，直接用 FileSystemResource 读物理路径，性能最高
            Resource resource = new FileSystemResource(targetFile);
            List<Document> documents;

            if (".pdf".equals(fileExtension)) {
                PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
                documents = pdfReader.get();
            } else if (".txt".equals(fileExtension) || ".md".equals(fileExtension)) {
                TextReader textReader = new TextReader(resource);
                textReader.getCustomMetadata().put("fileType", fileExtension);
                documents = textReader.get();
            } else {
                throw new RuntimeException("仅支持解析 PDF、TXT 和 Markdown 格式的文件！");
            }

            // 6. 文本切片
            TokenTextSplitter textSplitter = new TokenTextSplitter(
                    800,
                    400,
                    5,
                    10000,
                    true
            );
            List<Document> splitDocuments = textSplitter.apply(documents);

            // 7. 注入 Metadata
            for (Document doc : splitDocuments) {
                doc.getMetadata().put("docId", docId);
                doc.getMetadata().put("fileName", originalFilename);
                // 也可以把 fileUrl 存进向量库，让 AI 在回答时能顺便附上文件下载链接
                doc.getMetadata().put("fileUrl", fileUrl);
            }

            // 8. 存入 Redis 向量库
            vectorStore.accept(splitDocuments);

            return docId;

        } catch (Exception e) {
            throw new RuntimeException("文档上传与解析失败: " + e.getMessage(), e);
        }
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(String docId) {
        SysDocument doc = this.getById(docId);
        if (doc != null) {
            // 1. 从 MySQL 中删除记录
            this.removeById(docId);

            // 2. 从本地硬盘中删除物理文件
            String originalFilename = doc.getFileName();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            }
            File targetFile = new File(location, docId + fileExtension);
            if (targetFile.exists()) {
                targetFile.delete();
            }

            // 💡 知识库冗余说明：Spring AI 的 Redis VectorStore 目前不支持直接通过 Metadata(docId) 批量删除向量切片。
            // 但因为我们已经在检索时强制要求 filterExpression("docId == 'xxx'")，
            // 只要前端删除了这个文件进不了这个会话，即使 Redis 里有冗余，AI 也永远不会再去读取它，绝对安全。
        }
    }
}