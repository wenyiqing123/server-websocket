package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.exception.ServiceException;
import cn.wyq.serverwebsocket.service.AiServiceManagementService;
import cn.wyq.serverwebsocket.service.ServiceQuestionSuggestionService;
import cn.wyq.serverwebsocket.service.SysConfigService;
import cn.wyq.serverwebsocket.utils.SystemConfigUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 智能客服后台管理服务实现。
 *
 * <p>公共知识库采用“原文件私有保存、向量先写入、数据库后发布”的顺序。
 * 新版本处理失败时数据库仍指向旧版本，线上客服不会切换到半成品索引。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceManagementServiceImpl implements AiServiceManagementService {

    private static final String LEGACY_FILTER = "knowledgeBase == 'engineering'";
    private static final String VECTOR_CACHE_PATH = "web/src/main/resources/static/dataset/mecha_vector_cache.json";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".txt", ".md");
    private static final Set<String> SETTING_KEYS = Set.of(
            "ai_service_name", "ai_service_avatar", "ai_service_welcome", "ai_service_system_prompt",
            "ai_service_temperature", "ai_service_max_tokens", "ai_service_status",
            "ai_service_message_tool_enabled", "service_question_suggestion_enabled",
            "service_question_similarity_threshold", "service_question_max_results");
    private static final Map<String, String> SETTING_DEFAULTS = Map.ofEntries(
            Map.entry("ai_service_name", "云聊智能客服"),
            Map.entry("ai_service_avatar", ""),
            Map.entry("ai_service_welcome", "您好，我是云聊智能客服，请问有什么可以帮您？"),
            Map.entry("ai_service_system_prompt", "你是云聊官方智能客服。请结合知识库和聊天上下文准确回答用户问题。"),
            Map.entry("ai_service_temperature", "0.4"),
            Map.entry("ai_service_max_tokens", "4096"),
            Map.entry("ai_service_status", "true"),
            Map.entry("ai_service_message_tool_enabled", "true"),
            Map.entry("service_question_suggestion_enabled", "true"),
            Map.entry("service_question_similarity_threshold", "0.38"),
            Map.entry("service_question_max_results", "5"));

    private final JdbcTemplate jdbcTemplate;
    private final VectorStore vectorStore;
    private final SysConfigService sysConfigService;
    private final ServiceQuestionSuggestionService questionSuggestionService;

    /** 公共客服知识库的私有存储目录，默认位于应用工作目录。 */
    @Value("${ai.knowledge.storage.location:}")
    private String configuredStorageLocation;

    @Override
    public Map<String, Object> getPublicSettings() {
        Map<String, String> settings = getAdminSettings();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", settings.get("ai_service_name"));
        result.put("avatar", settings.get("ai_service_avatar"));
        result.put("welcome", settings.get("ai_service_welcome"));
        result.put("enabled", parseBoolean(settings.get("ai_service_status"), true));
        result.put("suggestionsEnabled", parseBoolean(settings.get("service_question_suggestion_enabled"), true));
        result.put("maxSuggestions", SystemConfigUtils.intValue(settings,
                "service_question_max_results", 5, 1, 10));
        return result;
    }

    @Override
    public Map<String, String> getAdminSettings() {
        Map<String, String> result = new LinkedHashMap<>(SETTING_DEFAULTS);
        try {
            Map<String, String> values = sysConfigService.getConfigMap();
            SETTING_KEYS.forEach(key -> {
                if (StringUtils.hasText(values.get(key))) {
                    result.put(key, values.get(key));
                }
            });
        } catch (RuntimeException exception) {
            log.warn("读取智能客服配置失败，将使用默认配置", exception);
        }
        return result;
    }

    @Override
    public void updateSettings(Map<String, String> settings, Integer operatorId) {
        if (settings == null || settings.isEmpty()) {
            throw new ServiceException("客服配置不能为空", 400);
        }
        Map<String, String> validated = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().trim();
            if (!SETTING_KEYS.contains(key)) {
                throw new ServiceException("不支持修改客服配置项：" + key, 400);
            }
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            validateSetting(key, value);
            validated.put(key, value);
        }
        sysConfigService.updateValues(validated);
        logOperation(operatorId, "UPDATE_SERVICE_SETTINGS", "SERVICE", "official", validated.keySet().toString());
    }

    @Override
    public Map<String, Object> getOverview() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, String> settings = getAdminSettings();
        result.put("enabled", parseBoolean(settings.get("ai_service_status"), true));
        result.put("mountedKnowledgeBases", scalar("SELECT COUNT(*) FROM ai_service_knowledge_binding b JOIN ai_knowledge_base k ON k.id=b.knowledge_base_id WHERE b.mounted=1 AND k.status=1 AND k.deleted=0"));
        result.put("publishedDocuments", scalar("SELECT COUNT(*) FROM ai_knowledge_document WHERE process_status='PUBLISHED' AND deleted=0"));
        result.put("openSessions", scalar("SELECT COUNT(*) FROM ai_chat_session WHERE LEFT(id,16)='service_session_' AND ticket_status IN ('OPEN','PROCESSING')"));
        result.put("questionCount", scalar("SELECT COUNT(*) FROM ai_service_question WHERE source_type='service' AND enabled=1 AND review_status='APPROVED'"));
        result.put("feedbackCount", scalar("SELECT COUNT(*) FROM ai_service_feedback"));
        result.put("helpfulRate", helpfulRate());
        return result;
    }

    @Override
    public List<Map<String, Object>> listKnowledgeBases() {
        return jdbcTemplate.queryForList("""
                SELECT k.id, k.name, k.description, k.status, k.source_type, k.current_version,
                       k.document_count, k.chunk_count, k.created_by, k.created_at, k.updated_at,
                       COALESCE(b.mounted, 0) AS mounted, COALESCE(b.priority, 0) AS priority
                FROM ai_knowledge_base k
                LEFT JOIN ai_service_knowledge_binding b ON b.knowledge_base_id = k.id
                WHERE k.deleted = 0
                ORDER BY COALESCE(b.mounted, 0) DESC, COALESCE(b.priority, 0) DESC, k.id DESC
                """);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createKnowledgeBase(Map<String, Object> body, Integer operatorId) {
        String name = requiredText(body, "name", "知识库名称", 100);
        String description = optionalText(body, "description", 500);
        jdbcTemplate.update("INSERT INTO ai_knowledge_base(name,description,status,source_type,current_version,document_count,chunk_count,created_by,deleted,created_at,updated_at) VALUES(?,?,1,'UPLOAD',0,0,0,?,0,NOW(),NOW())",
                name, description, operatorId);
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        logOperation(operatorId, "CREATE_KNOWLEDGE_BASE", "KNOWLEDGE_BASE", String.valueOf(id), name);
        return jdbcTemplate.queryForMap("SELECT * FROM ai_knowledge_base WHERE id=?", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateKnowledgeBase(Long id, Map<String, Object> body, Integer operatorId) {
        requireKnowledgeBase(id);
        String name = requiredText(body, "name", "知识库名称", 100);
        String description = optionalText(body, "description", 500);
        int status = booleanNumber(body == null ? null : body.get("status"), true);
        jdbcTemplate.update("UPDATE ai_knowledge_base SET name=?,description=?,status=?,updated_at=NOW() WHERE id=? AND deleted=0",
                name, description, status, id);
        logOperation(operatorId, "UPDATE_KNOWLEDGE_BASE", "KNOWLEDGE_BASE", String.valueOf(id), name);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeBase(Long id, Integer operatorId) {
        requireKnowledgeBase(id);
        Integer documents = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_knowledge_document WHERE knowledge_base_id=? AND deleted=0", Integer.class, id);
        if (documents != null && documents > 0) {
            throw new ServiceException("请先删除知识库中的文档", 409);
        }
        jdbcTemplate.update("DELETE FROM ai_service_knowledge_binding WHERE knowledge_base_id=?", id);
        jdbcTemplate.update("UPDATE ai_knowledge_base SET deleted=1,status=0,updated_at=NOW() WHERE id=?", id);
        logOperation(operatorId, "DELETE_KNOWLEDGE_BASE", "KNOWLEDGE_BASE", String.valueOf(id), "软删除知识库");
    }

    @Override
    public void setKnowledgeBaseMounted(Long id, boolean mounted, Integer operatorId) {
        requireKnowledgeBase(id);
        jdbcTemplate.update("""
                INSERT INTO ai_service_knowledge_binding(knowledge_base_id,mounted,priority,updated_by,created_at,updated_at)
                VALUES(?,?,0,?,NOW(),NOW())
                ON DUPLICATE KEY UPDATE mounted=VALUES(mounted),updated_by=VALUES(updated_by),updated_at=NOW()
                """, id, mounted ? 1 : 0, operatorId);
        logOperation(operatorId, mounted ? "MOUNT_KNOWLEDGE_BASE" : "UNMOUNT_KNOWLEDGE_BASE",
                "KNOWLEDGE_BASE", String.valueOf(id), "mounted=" + mounted);
    }

    @Override
    public List<Map<String, Object>> listKnowledgeDocuments(Long knowledgeBaseId) {
        requireKnowledgeBase(knowledgeBaseId);
        return jdbcTemplate.queryForList("""
                SELECT id,document_uuid,knowledge_base_id,file_name,file_size,content_hash,file_type,
                       process_status,current_version,chunk_count,duplicate_count,error_message,
                       created_by,created_at,updated_at
                FROM ai_knowledge_document
                WHERE knowledge_base_id=? AND deleted=0
                ORDER BY id DESC
                """, knowledgeBaseId);
    }

    @Override
    public Map<String, Object> uploadKnowledgeDocument(Long knowledgeBaseId, MultipartFile file, Integer operatorId) {
        requireKnowledgeBase(knowledgeBaseId);
        validateFile(file);
        String originalName = normalizeFileName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        String hash = sha256(file);
        Integer duplicate = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_knowledge_document WHERE knowledge_base_id=? AND content_hash=? AND deleted=0",
                Integer.class, knowledgeBaseId, hash);
        if (duplicate != null && duplicate > 0) {
            throw new ServiceException("该知识库中已存在内容相同的文件", 409);
        }
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String storageKey = uuid + extension;
        Path storedFile = storageRoot().resolve(storageKey).normalize();
        if (!storedFile.startsWith(storageRoot())) {
            throw new ServiceException("文件存储路径无效", 400);
        }
        try {
            Files.createDirectories(storageRoot());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, storedFile, StandardCopyOption.REPLACE_EXISTING);
            }
            jdbcTemplate.update("""
                    INSERT INTO ai_knowledge_document(document_uuid,knowledge_base_id,file_name,storage_key,file_size,
                      content_hash,file_type,process_status,current_version,chunk_count,duplicate_count,created_by,deleted,created_at,updated_at)
                    VALUES(?,?,?,?,?,?,?,'PARSING',0,0,0,?,0,NOW(),NOW())
                    """, uuid, knowledgeBaseId, originalName, storageKey, file.getSize(), hash,
                    extension.substring(1).toUpperCase(Locale.ROOT), operatorId);
            Long documentId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            publishDocument(documentId, knowledgeBaseId, originalName, storedFile, 1, 0, operatorId);
            logOperation(operatorId, "UPLOAD_KNOWLEDGE_DOCUMENT", "KNOWLEDGE_DOCUMENT", String.valueOf(documentId), originalName);
            return jdbcTemplate.queryForMap("SELECT * FROM ai_knowledge_document WHERE id=?", documentId);
        } catch (ServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            try {
                Files.deleteIfExists(storedFile);
            } catch (IOException ignored) {
                log.warn("清理上传失败的知识库文件失败: {}", storedFile);
            }
            throw new ServiceException("知识库文档上传失败：" + rootMessage(exception), 500);
        }
    }

    @Override
    public void renameKnowledgeDocument(Long documentId, String fileName, Integer operatorId) {
        Map<String, Object> document = requireDocument(documentId);
        String normalizedName = normalizeFileName(fileName);
        String oldExtension = "." + String.valueOf(document.get("file_type")).toLowerCase(Locale.ROOT);
        if (!normalizedName.toLowerCase(Locale.ROOT).endsWith(oldExtension)) {
            normalizedName += oldExtension;
        }
        jdbcTemplate.update("UPDATE ai_knowledge_document SET file_name=?,updated_at=NOW() WHERE id=?", normalizedName, documentId);
        logOperation(operatorId, "RENAME_KNOWLEDGE_DOCUMENT", "KNOWLEDGE_DOCUMENT", String.valueOf(documentId), normalizedName);
    }

    @Override
    public void reindexKnowledgeDocument(Long documentId, Integer operatorId) {
        Map<String, Object> document = requireDocument(documentId);
        long knowledgeBaseId = longValue(document.get("knowledge_base_id"));
        int oldVersion = intValue(document.get("current_version"));
        int nextVersion = Math.max(1, oldVersion + 1);
        Path storedFile = resolveStoredFile(String.valueOf(document.get("storage_key")));
        publishDocument(documentId, knowledgeBaseId, String.valueOf(document.get("file_name")),
                storedFile, nextVersion, oldVersion, operatorId);
        deleteDocumentVectors(documentId, oldVersion, intValue(document.get("chunk_count")));
        persistVectorStore();
        logOperation(operatorId, "REINDEX_KNOWLEDGE_DOCUMENT", "KNOWLEDGE_DOCUMENT", String.valueOf(documentId), "version=" + nextVersion);
    }

    @Override
    public void deleteKnowledgeDocument(Long documentId, Integer operatorId) {
        Map<String, Object> document = requireDocument(documentId);
        long knowledgeBaseId = longValue(document.get("knowledge_base_id"));
        // 先让动态过滤器排除该文档，再清理向量；即使缓存文件删除失败也不会继续被客服检索。
        jdbcTemplate.update("UPDATE ai_knowledge_document SET deleted=1,process_status='DELETED',updated_at=NOW() WHERE id=?", documentId);
        jdbcTemplate.update("DELETE FROM ai_knowledge_chunk_hash WHERE document_id=?", documentId);
        refreshKnowledgeBaseCounters(knowledgeBaseId);
        deleteDocumentVectors(documentId, intValue(document.get("current_version")), intValue(document.get("chunk_count")));
        persistVectorStore();
        try {
            Files.deleteIfExists(resolveStoredFile(String.valueOf(document.get("storage_key"))));
        } catch (IOException exception) {
            log.warn("知识库记录已删除，但原始文件清理失败，documentId={}", documentId, exception);
        }
        logOperation(operatorId, "DELETE_KNOWLEDGE_DOCUMENT", "KNOWLEDGE_DOCUMENT", String.valueOf(documentId), String.valueOf(document.get("file_name")));
    }

    @Override
    public DownloadableKnowledgeDocument loadKnowledgeDocument(Long documentId) {
        Map<String, Object> document = requireDocument(documentId);
        Path storedFile = resolveStoredFile(String.valueOf(document.get("storage_key")));
        if (!Files.isRegularFile(storedFile)) {
            throw new ServiceException("知识库原始文件不存在", 404);
        }
        try {
            return new DownloadableKnowledgeDocument(new FileSystemResource(storedFile),
                    String.valueOf(document.get("file_name")), Files.size(storedFile));
        } catch (IOException exception) {
            throw new ServiceException("读取知识库文件失败", 500);
        }
    }

    @Override
    public List<Map<String, Object>> testRetrieval(String query, int topK) {
        if (!StringUtils.hasText(query)) {
            throw new ServiceException("检索问题不能为空", 400);
        }
        SearchRequest request = SearchRequest.builder()
                .query(query.trim())
                .topK(Math.max(1, Math.min(topK, 20)))
                .similarityThreshold(0D)
                .filterExpression(resolveMountedKnowledgeFilter())
                .build();
        List<Document> documents = vectorStore.similaritySearch(request);
        if (documents == null) {
            return List.of();
        }
        return documents.stream().map(document -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("content", document.getText());
            item.put("score", document.getScore());
            item.put("fileName", document.getMetadata().getOrDefault("fileName",
                    document.getMetadata().getOrDefault("source", "kg.txt")));
            item.put("knowledgeBaseId", document.getMetadata().get("knowledgeBaseId"));
            item.put("documentId", document.getMetadata().get("knowledgeDocumentId"));
            return item;
        }).toList();
    }

    @Override
    public String resolveMountedKnowledgeFilter() {
        try {
            Integer legacyMounted = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM ai_knowledge_base k
                    JOIN ai_service_knowledge_binding b ON b.knowledge_base_id=k.id
                    WHERE b.mounted=1 AND k.status=1 AND k.deleted=0 AND k.source_type='LEGACY'
                    """, Integer.class);
            List<Map<String, Object>> documents = jdbcTemplate.queryForList("""
                    SELECT d.id,d.current_version FROM ai_knowledge_document d
                    JOIN ai_knowledge_base k ON k.id=d.knowledge_base_id
                    JOIN ai_service_knowledge_binding b ON b.knowledge_base_id=k.id
                    WHERE b.mounted=1 AND k.status=1 AND k.deleted=0
                      AND d.deleted=0 AND d.process_status='PUBLISHED' AND d.current_version>0
                    ORDER BY b.priority DESC,k.id,d.id
                    """);
            List<String> filters = new ArrayList<>();
            if (legacyMounted != null && legacyMounted > 0) {
                filters.add(LEGACY_FILTER);
            }
            for (Map<String, Object> document : documents) {
                filters.add("(knowledgeDocumentId == '" + document.get("id")
                        + "' && knowledgeVersion == '" + document.get("current_version") + "')");
            }
            return filters.isEmpty()
                    ? "knowledgeBase == '__no_mounted_knowledge__'"
                    : String.join(" || ", new LinkedHashSet<>(filters));
        } catch (DataAccessException exception) {
            log.warn("知识库迁移表尚不可用，客服继续使用内置 kg.txt", exception);
            return LEGACY_FILTER;
        }
    }

    @Override
    public List<Map<String, Object>> listSessions(String keyword, String status) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? "%" + keyword.trim() + "%" : "%";
        String normalizedStatus = StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : "%";
        return jdbcTemplate.queryForList("""
                SELECT s.id,s.owner_user_id,s.user_id,u.user_name,s.title,s.ticket_status,s.admin_remark,
                       s.last_message_at,s.closed_at,s.created_at,
                       (SELECT COUNT(*) FROM ai_chat_memory m WHERE m.conversation_id=CONCAT('service:',s.id)) AS message_count
                FROM ai_chat_session s LEFT JOIN user u ON u.id=s.owner_user_id
                WHERE LEFT(s.id,16)='service_session_'
                  AND (s.id LIKE ? OR s.title LIKE ? OR s.user_id LIKE ? OR u.user_name LIKE ?)
                  AND s.ticket_status LIKE ?
                ORDER BY COALESCE(s.last_message_at,s.created_at) DESC LIMIT 500
                """, normalizedKeyword, normalizedKeyword, normalizedKeyword, normalizedKeyword, normalizedStatus);
    }

    @Override
    public List<Map<String, Object>> getSessionMessages(String sessionId) {
        requireServiceSession(sessionId);
        return jdbcTemplate.queryForList("SELECT id,type,content,timestamp FROM ai_chat_memory WHERE conversation_id=? ORDER BY timestamp,id",
                "service:" + sessionId);
    }

    @Override
    public void updateSession(String sessionId, Map<String, Object> body, Integer operatorId) {
        requireServiceSession(sessionId);
        String status = optionalText(body, "ticketStatus", 20).toUpperCase(Locale.ROOT);
        if (!Set.of("OPEN", "PROCESSING", "CLOSED").contains(status)) {
            throw new ServiceException("工单状态不正确", 400);
        }
        String remark = optionalText(body, "adminRemark", 1000);
        jdbcTemplate.update("UPDATE ai_chat_session SET ticket_status=?,admin_remark=?,closed_at=IF(?='CLOSED',NOW(),NULL) WHERE id=?",
                status, remark, status, sessionId);
        logOperation(operatorId, "UPDATE_SERVICE_SESSION", "SESSION", sessionId, status + ":" + remark);
    }

    @Override
    public List<Map<String, Object>> listQuestions(String keyword, String reviewStatus, Boolean enabled) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? "%" + keyword.trim() + "%" : "%";
        String normalizedReview = StringUtils.hasText(reviewStatus) ? reviewStatus.trim().toUpperCase(Locale.ROOT) : "%";
        int enabledFilter = enabled == null ? -1 : (enabled ? 1 : 0);
        return jdbcTemplate.queryForList("""
                SELECT id,question,source_type,use_count,last_user_id,last_used_at,enabled,review_status,admin_remark,created_at,updated_at
                FROM ai_service_question
                WHERE source_type='service' AND question LIKE ? AND review_status LIKE ? AND (?=-1 OR enabled=?)
                ORDER BY use_count DESC,updated_at DESC LIMIT 1000
                """, normalizedKeyword, normalizedReview, enabledFilter, enabledFilter);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createQuestion(Map<String, Object> body, Integer operatorId) {
        String question = requiredText(body, "question", "问题内容", 2000);
        String normalized = normalizeQuestion(question);
        jdbcTemplate.update("""
                INSERT INTO ai_service_question(question_hash,question,normalized_question,source_type,use_count,last_user_id,last_used_at,enabled,review_status,admin_remark,created_at,updated_at)
                VALUES(?,?,?,'service',0,NULL,NULL,1,'APPROVED',?,NOW(),NOW())
                ON DUPLICATE KEY UPDATE question=VALUES(question),enabled=1,review_status='APPROVED',admin_remark=VALUES(admin_remark),updated_at=NOW()
                """, sha256(normalized), question, normalized, optionalText(body, "adminRemark", 1000));
        questionSuggestionService.refreshCache();
        logOperation(operatorId, "CREATE_SERVICE_QUESTION", "QUESTION", "new", question);
    }

    @Override
    public void updateQuestion(Long id, Map<String, Object> body, Integer operatorId) {
        String question = requiredText(body, "question", "问题内容", 2000);
        String normalized = normalizeQuestion(question);
        String reviewStatus = optionalText(body, "reviewStatus", 20).toUpperCase(Locale.ROOT);
        if (!Set.of("PENDING", "APPROVED", "REJECTED").contains(reviewStatus)) {
            throw new ServiceException("审核状态不正确", 400);
        }
        int enabled = booleanNumber(body == null ? null : body.get("enabled"), true);
        int updated = jdbcTemplate.update("UPDATE ai_service_question SET question_hash=?,question=?,normalized_question=?,enabled=?,review_status=?,admin_remark=?,updated_at=NOW() WHERE id=? AND source_type='service'",
                sha256(normalized), question, normalized, enabled, reviewStatus, optionalText(body, "adminRemark", 1000), id);
        if (updated == 0) {
            throw new ServiceException("问题记录不存在", 404);
        }
        questionSuggestionService.refreshCache();
        logOperation(operatorId, "UPDATE_SERVICE_QUESTION", "QUESTION", String.valueOf(id), reviewStatus);
    }

    @Override
    public void deleteQuestion(Long id, Integer operatorId) {
        int deleted = jdbcTemplate.update("DELETE FROM ai_service_question WHERE id=? AND source_type='service'", id);
        if (deleted == 0) {
            throw new ServiceException("问题记录不存在", 404);
        }
        questionSuggestionService.refreshCache();
        logOperation(operatorId, "DELETE_SERVICE_QUESTION", "QUESTION", String.valueOf(id), "删除问题提示");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mergeQuestions(Long targetId, List<Long> sourceIds, Integer operatorId) {
        if (targetId == null || sourceIds == null || sourceIds.isEmpty()) {
            throw new ServiceException("请选择保留问题和待合并问题", 400);
        }
        List<Long> normalizedIds = sourceIds.stream().filter(Objects::nonNull).filter(id -> !id.equals(targetId)).distinct().toList();
        if (normalizedIds.isEmpty()) {
            throw new ServiceException("没有可合并的问题", 400);
        }
        Integer targetExists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_service_question WHERE id=? AND source_type='service'", Integer.class, targetId);
        if (targetExists == null || targetExists == 0) {
            throw new ServiceException("保留的问题不存在", 404);
        }
        String placeholders = String.join(",", Collections.nCopies(normalizedIds.size(), "?"));
        jdbcTemplate.update("UPDATE ai_service_question SET use_count=use_count+(SELECT COALESCE(SUM(use_count),0) FROM (SELECT use_count FROM ai_service_question WHERE id IN (" + placeholders + ")) q),updated_at=NOW() WHERE id=?",
                concatArgs(normalizedIds, targetId));
        jdbcTemplate.update("DELETE FROM ai_service_question WHERE id IN (" + placeholders + ") AND source_type='service'", normalizedIds.toArray());
        questionSuggestionService.refreshCache();
        logOperation(operatorId, "MERGE_SERVICE_QUESTIONS", "QUESTION", String.valueOf(targetId), normalizedIds.toString());
    }

    @Override
    public void createFeedback(String sessionId, Long messageId, Integer userId, boolean helpful, String reason) {
        if (userId == null || userId <= 0) {
            throw new ServiceException("请先登录", 401);
        }
        Map<String, Object> session = requireServiceSession(sessionId);
        if (!Objects.equals(userId, intValue(session.get("owner_user_id")))) {
            throw new ServiceException("无权评价该工单", 403);
        }
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.length() > 1000) {
            throw new ServiceException("反馈原因不能超过1000字", 400);
        }
        jdbcTemplate.update("""
                INSERT INTO ai_service_feedback(session_id,message_id,user_id,helpful,reason,created_at,updated_at)
                VALUES(?,?,?,?,?,NOW(),NOW())
                ON DUPLICATE KEY UPDATE helpful=VALUES(helpful),reason=VALUES(reason),updated_at=NOW()
                """, sessionId, messageId, userId, helpful ? 1 : 0, normalizedReason);
    }

    @Override
    public List<Map<String, Object>> listFeedback(Boolean helpful) {
        int filter = helpful == null ? -1 : (helpful ? 1 : 0);
        return jdbcTemplate.queryForList("""
                SELECT f.id,f.session_id,f.message_id,f.user_id,u.user_name,f.helpful,f.reason,f.created_at,f.updated_at
                FROM ai_service_feedback f LEFT JOIN user u ON u.id=f.user_id
                WHERE (?=-1 OR f.helpful=?) ORDER BY f.updated_at DESC LIMIT 1000
                """, filter, filter);
    }

    @Override
    public List<Map<String, Object>> listUnmatchedQuestions() {
        return jdbcTemplate.queryForList("""
                SELECT l.id,l.user_id,u.user_name,l.question,l.matched_question,l.similarity,l.created_at
                FROM ai_service_question_log l LEFT JOIN user u ON u.id=l.user_id
                WHERE l.matched=0 ORDER BY l.id DESC LIMIT 1000
                """);
    }

    @Override
    public List<Map<String, Object>> listOperationLogs() {
        return jdbcTemplate.queryForList("""
                SELECT l.id,l.operator_id,u.user_name AS operator_name,l.action,l.target_type,l.target_id,l.change_summary,l.created_at
                FROM ai_service_operation_log l LEFT JOIN user u ON u.id=l.operator_id
                ORDER BY l.id DESC LIMIT 1000
                """);
    }

    private void publishDocument(Long documentId, long knowledgeBaseId, String fileName,
                                 Path storedFile, int version, int previousVersion,
                                 Integer operatorId) {
        List<Document> chunks = List.of();
        boolean vectorsAdded = false;
        try {
            if (previousVersion <= 0) {
                jdbcTemplate.update("UPDATE ai_knowledge_document SET process_status='PARSING',error_message=NULL,updated_at=NOW() WHERE id=?", documentId);
            } else {
                jdbcTemplate.update("UPDATE ai_knowledge_document SET error_message=NULL,updated_at=NOW() WHERE id=?", documentId);
            }
            chunks = parseAndSplit(storedFile);
            int duplicateCount = removeDuplicateChunks(chunks, knowledgeBaseId, documentId);
            for (int index = 0; index < chunks.size(); index++) {
                Document source = chunks.get(index);
                Map<String, Object> metadata = new HashMap<>(source.getMetadata());
                metadata.put("knowledgeBase", "service-managed");
                metadata.put("knowledgeBaseId", String.valueOf(knowledgeBaseId));
                metadata.put("knowledgeDocumentId", String.valueOf(documentId));
                metadata.put("knowledgeVersion", String.valueOf(version));
                metadata.put("fileName", fileName);
                chunks.set(index, new Document(vectorId(documentId, version, index), source.getText(), metadata));
            }
            if (previousVersion <= 0) {
                jdbcTemplate.update("UPDATE ai_knowledge_document SET process_status='VECTORIZING',updated_at=NOW() WHERE id=?", documentId);
            }
            synchronized (vectorStore) {
                vectorStore.add(chunks);
                persistVectorStore();
            }
            vectorsAdded = true;
            jdbcTemplate.update("UPDATE ai_knowledge_document SET process_status='PUBLISHED',current_version=?,chunk_count=?,duplicate_count=?,error_message=NULL,updated_at=NOW() WHERE id=?",
                    version, chunks.size(), duplicateCount, documentId);
            // 文档发布语句成功后，新版本已经是线上版本，后续统计或去重索引失败不能回滚向量。
            vectorsAdded = false;
            try {
                persistChunkHashes(documentId, knowledgeBaseId, version, chunks);
                jdbcTemplate.update("UPDATE ai_knowledge_base SET current_version=GREATEST(current_version,?),updated_at=NOW() WHERE id=?", version, knowledgeBaseId);
                refreshKnowledgeBaseCounters(knowledgeBaseId);
            } catch (DataAccessException maintenanceException) {
                log.warn("知识库已发布，但辅助统计或重复知识索引刷新失败，documentId={}", documentId, maintenanceException);
            }
        } catch (Exception exception) {
            if (vectorsAdded) {
                try {
                    synchronized (vectorStore) {
                        vectorStore.delete(chunks.stream().map(Document::getId).toList());
                        persistVectorStore();
                    }
                } catch (RuntimeException cleanupException) {
                    log.warn("清理未发布的知识库向量失败，documentId={}", documentId, cleanupException);
                }
            }
            String error = rootMessage(exception);
            jdbcTemplate.update("UPDATE ai_knowledge_document SET process_status=?,error_message=?,updated_at=NOW() WHERE id=?",
                    previousVersion > 0 ? "PUBLISHED" : "FAILED",
                    error.length() > 2000 ? error.substring(0, 2000) : error, documentId);
            logOperation(operatorId, "INDEX_KNOWLEDGE_DOCUMENT_FAILED", "KNOWLEDGE_DOCUMENT", String.valueOf(documentId), error);
            throw exception instanceof ServiceException serviceException
                    ? serviceException : new ServiceException("知识库索引失败：" + error, 500);
        }
    }

    private List<Document> parseAndSplit(Path storedFile) {
        if (!Files.isRegularFile(storedFile)) {
            throw new ServiceException("知识库原始文件不存在", 404);
        }
        Resource resource = new FileSystemResource(storedFile);
        List<Document> sourceDocuments;
        if (storedFile.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            sourceDocuments = new PagePdfDocumentReader(resource).get();
        } else {
            sourceDocuments = new TextReader(resource).get();
        }
        int characters = sourceDocuments.stream().filter(Objects::nonNull).map(Document::getText)
                .filter(Objects::nonNull).mapToInt(String::length).sum();
        if (characters == 0) {
            throw new ServiceException("文档未提取到可用文本", 422);
        }
        Map<String, String> config = sysConfigService.getConfigMap();
        int chunkSize = SystemConfigUtils.intValue(config, "rag_chunk_size", 800, 100, 10_000);
        int maxSlices = SystemConfigUtils.intValue(config, "rag_max_slices", 400, 1, 10_000);
        TokenTextSplitter splitter = new TokenTextSplitter(chunkSize,
                Math.max(1, Math.min(350, chunkSize - 1)), 5, 10_000, true);
        List<Document> chunks = new ArrayList<>(splitter.apply(sourceDocuments));
        chunks.removeIf(document -> document == null || !StringUtils.hasText(document.getText()));
        if (chunks.isEmpty() || chunks.size() > maxSlices) {
            throw new ServiceException(chunks.isEmpty() ? "文档未生成可用切片" : "文档切片数量超过限制 " + maxSlices, 422);
        }
        return chunks;
    }

    private int removeDuplicateChunks(List<Document> chunks, long knowledgeBaseId, Long documentId) {
        Set<String> hashes = new LinkedHashSet<>();
        int originalSize = chunks.size();
        chunks.removeIf(document -> {
            String hash = sha256(document.getText().trim());
            if (!hashes.add(hash)) {
                return true;
            }
            try {
                Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_knowledge_chunk_hash WHERE knowledge_base_id=? AND content_hash=? AND document_id<>?",
                        Integer.class, knowledgeBaseId, hash, documentId);
                return count != null && count > 0;
            } catch (DataAccessException exception) {
                // 迁移未执行时仍保留文件级和文档内去重，避免影响原客服功能。
                return false;
            }
        });
        if (chunks.isEmpty()) {
            throw new ServiceException("文档内容与该知识库中的现有知识完全重复", 409);
        }
        return originalSize - chunks.size();
    }

    private void persistChunkHashes(Long documentId, long knowledgeBaseId, int version, List<Document> chunks) {
        jdbcTemplate.update("DELETE FROM ai_knowledge_chunk_hash WHERE document_id=?", documentId);
        for (int index = 0; index < chunks.size(); index++) {
            jdbcTemplate.update("INSERT INTO ai_knowledge_chunk_hash(knowledge_base_id,document_id,document_version,chunk_index,content_hash,created_at) VALUES(?,?,?,?,?,NOW())",
                    knowledgeBaseId, documentId, version, index, sha256(chunks.get(index).getText().trim()));
        }
    }

    private void deleteDocumentVectors(Long documentId, int version, int chunkCount) {
        if (version <= 0 || chunkCount <= 0) {
            return;
        }
        List<String> ids = new ArrayList<>(chunkCount);
        for (int index = 0; index < chunkCount; index++) {
            ids.add(vectorId(documentId, version, index));
        }
        synchronized (vectorStore) {
            vectorStore.delete(ids);
        }
    }

    private String vectorId(Long documentId, int version, int index) {
        return "service-kb-" + documentId + "-v" + version + "-" + index;
    }

    private void persistVectorStore() {
        if (!(vectorStore instanceof SimpleVectorStore simpleVectorStore)) {
            return;
        }
        File file = new File(VECTOR_CACHE_PATH);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new ServiceException("无法创建向量缓存目录", 500);
        }
        simpleVectorStore.save(file);
    }

    private void refreshKnowledgeBaseCounters(long knowledgeBaseId) {
        jdbcTemplate.update("""
                UPDATE ai_knowledge_base k SET
                  document_count=(SELECT COUNT(*) FROM ai_knowledge_document d WHERE d.knowledge_base_id=k.id AND d.deleted=0 AND d.process_status='PUBLISHED'),
                  chunk_count=(SELECT COALESCE(SUM(d.chunk_count),0) FROM ai_knowledge_document d WHERE d.knowledge_base_id=k.id AND d.deleted=0 AND d.process_status='PUBLISHED'),
                  updated_at=NOW()
                WHERE k.id=?
                """, knowledgeBaseId);
    }

    private Map<String, Object> requireKnowledgeBase(Long id) {
        if (id == null || id <= 0) {
            throw new ServiceException("知识库编号无效", 400);
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM ai_knowledge_base WHERE id=? AND deleted=0", id);
        if (rows.isEmpty()) {
            throw new ServiceException("知识库不存在", 404);
        }
        return rows.get(0);
    }

    private Map<String, Object> requireDocument(Long id) {
        if (id == null || id <= 0) {
            throw new ServiceException("知识库文档编号无效", 400);
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM ai_knowledge_document WHERE id=? AND deleted=0", id);
        if (rows.isEmpty()) {
            throw new ServiceException("知识库文档不存在", 404);
        }
        return rows.get(0);
    }

    private Map<String, Object> requireServiceSession(String sessionId) {
        if (!StringUtils.hasText(sessionId) || !sessionId.startsWith("service_session_")) {
            throw new ServiceException("客服工单编号无效", 400);
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM ai_chat_session WHERE id=?", sessionId);
        if (rows.isEmpty()) {
            throw new ServiceException("客服工单不存在", 404);
        }
        return rows.get(0);
    }

    private Path storageRoot() {
        String location = StringUtils.hasText(configuredStorageLocation)
                ? configuredStorageLocation.trim() : "private-ai-knowledge";
        return Paths.get(location).toAbsolutePath().normalize();
    }

    private Path resolveStoredFile(String storageKey) {
        if (!StringUtils.hasText(storageKey) || !storageKey.matches("^[a-f0-9]{32}\\.(pdf|txt|md)$")) {
            throw new ServiceException("知识库文件存储键无效", 400);
        }
        Path target = storageRoot().resolve(storageKey).normalize();
        if (!target.startsWith(storageRoot())) {
            throw new ServiceException("知识库文件路径无效", 400);
        }
        return target;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请选择要上传的知识库文件", 400);
        }
        long maxBytes = SystemConfigUtils.longValue(sysConfigService.getConfigMap(), "rag_max_upload_bytes",
                10_485_760L, 1_024L, 1_073_741_824L);
        if (file.getSize() > maxBytes) {
            throw new ServiceException("文件大小超过知识库上传限制", 400);
        }
        if (!ALLOWED_EXTENSIONS.contains(extensionOf(normalizeFileName(file.getOriginalFilename())))) {
            throw new ServiceException("知识库仅支持 PDF、TXT 和 Markdown 文件", 400);
        }
    }

    private String normalizeFileName(String fileName) {
        String normalized = fileName == null ? "" : Paths.get(fileName).getFileName().toString().trim();
        normalized = normalized.replaceAll("[\\r\\n\\t]", " ");
        if (!StringUtils.hasText(normalized) || normalized.length() > 255) {
            throw new ServiceException("文件名无效或超过255个字符", 400);
        }
        return normalized;
    }

    private String extensionOf(String fileName) {
        String normalized = fileName.toLowerCase(Locale.ROOT);
        int dot = normalized.lastIndexOf('.');
        return dot < 0 ? "" : normalized.substring(dot);
    }

    private String sha256(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw new ServiceException("无法校验上传文件", 400);
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("无法计算内容摘要", exception);
        }
    }

    private void validateSetting(String key, String value) {
        if ((key.endsWith("status") || key.endsWith("enabled"))
                && !("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)
                || "1".equals(value) || "0".equals(value))) {
            throw new ServiceException("开关配置必须是布尔值", 400);
        }
        if (key.endsWith("temperature") || key.endsWith("threshold")) {
            try {
                double number = Double.parseDouble(value);
                if (number < 0D || number > 1D) {
                    throw new ServiceException("小数配置必须在0到1之间", 400);
                }
            } catch (NumberFormatException exception) {
                throw new ServiceException("小数配置格式不正确", 400);
            }
        }
        if (key.endsWith("max_tokens") || key.endsWith("max_results")) {
            try {
                int number = Integer.parseInt(value);
                int max = key.endsWith("max_results") ? 10 : 8192;
                if (number < 1 || number > max) {
                    throw new ServiceException("整数配置超出允许范围", 400);
                }
            } catch (NumberFormatException exception) {
                throw new ServiceException("整数配置格式不正确", 400);
            }
        }
        int maxLength = key.endsWith("system_prompt") ? 20_000 : (key.endsWith("welcome") ? 2_000 : 500);
        if (value.length() > maxLength) {
            throw new ServiceException("配置内容长度超出限制", 400);
        }
    }

    private String requiredText(Map<String, Object> body, String key, String label, int maxLength) {
        String value = optionalText(body, key, maxLength);
        if (!StringUtils.hasText(value)) {
            throw new ServiceException(label + "不能为空", 400);
        }
        return value;
    }

    private String optionalText(Map<String, Object> body, String key, int maxLength) {
        Object raw = body == null ? null : body.get(key);
        String value = raw == null ? "" : String.valueOf(raw).trim();
        if (value.length() > maxLength) {
            throw new ServiceException(key + "长度超出限制", 400);
        }
        return value;
    }

    private int booleanNumber(Object raw, boolean fallback) {
        if (raw == null) {
            return fallback ? 1 : 0;
        }
        if (raw instanceof Number number) {
            return number.intValue() == 0 ? 0 : 1;
        }
        return parseBoolean(String.valueOf(raw), fallback) ? 1 : 0;
    }

    private boolean parseBoolean(String value, boolean fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    private int scalar(String sql) {
        try {
            Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
            return value == null ? 0 : value;
        } catch (DataAccessException exception) {
            return 0;
        }
    }

    private double helpfulRate() {
        try {
            Double value = jdbcTemplate.queryForObject("SELECT COALESCE(ROUND(AVG(helpful)*100,2),0) FROM ai_service_feedback", Double.class);
            return value == null ? 0D : value;
        } catch (DataAccessException exception) {
            return 0D;
        }
    }

    private void logOperation(Integer operatorId, String action, String targetType, String targetId, String summary) {
        try {
            jdbcTemplate.update("INSERT INTO ai_service_operation_log(operator_id,action,target_type,target_id,change_summary,created_at) VALUES(?,?,?,?,?,NOW())",
                    operatorId, action, targetType, targetId, summary == null ? "" : summary);
        } catch (DataAccessException exception) {
            log.warn("智能客服审计日志写入失败，action={}, targetId={}", action, targetId, exception);
        }
    }

    private String normalizeQuestion(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[\\p{P}\\p{S}\\s]+", "");
    }

    private Object[] concatArgs(List<Long> ids, Long targetId) {
        List<Object> args = new ArrayList<>(ids);
        args.add(targetId);
        return args.toArray();
    }

    private int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
    }

    private long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return StringUtils.hasText(current.getMessage()) ? current.getMessage() : current.getClass().getSimpleName();
    }
}
