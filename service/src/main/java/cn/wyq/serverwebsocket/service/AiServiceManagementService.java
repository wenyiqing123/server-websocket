package cn.wyq.serverwebsocket.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 智能客服后台管理服务，统一承载客服配置、公共知识库、工单和质量运营能力。
 */
public interface AiServiceManagementService {

    /** 返回用户端可见且不会泄露系统提示词的客服配置。 */
    Map<String, Object> getPublicSettings();

    /** 返回管理员可维护的完整客服配置。 */
    Map<String, String> getAdminSettings();

    /** 保存管理员提交的客服配置。 */
    void updateSettings(Map<String, String> settings, Integer operatorId);

    /** 返回管理页运行概览。 */
    Map<String, Object> getOverview();

    /** 查询公共知识库。 */
    List<Map<String, Object>> listKnowledgeBases();

    /** 新增公共知识库。 */
    Map<String, Object> createKnowledgeBase(Map<String, Object> body, Integer operatorId);

    /** 修改知识库名称、说明或启用状态。 */
    void updateKnowledgeBase(Long id, Map<String, Object> body, Integer operatorId);

    /** 软删除空知识库。 */
    void deleteKnowledgeBase(Long id, Integer operatorId);

    /** 挂载或卸载客服知识库。 */
    void setKnowledgeBaseMounted(Long id, boolean mounted, Integer operatorId);

    /** 查询指定知识库的文档。 */
    List<Map<String, Object>> listKnowledgeDocuments(Long knowledgeBaseId);

    /** 上传、解析并发布一个公共知识库文档。 */
    Map<String, Object> uploadKnowledgeDocument(Long knowledgeBaseId, MultipartFile file, Integer operatorId);

    /** 修改知识库文档的展示名称。 */
    void renameKnowledgeDocument(Long documentId, String fileName, Integer operatorId);

    /** 使用已保存的原始文件重新构建并发布向量。 */
    void reindexKnowledgeDocument(Long documentId, Integer operatorId);

    /** 删除知识库文档及其当前向量。 */
    void deleteKnowledgeDocument(Long documentId, Integer operatorId);

    /** 加载知识库原始文件供管理员下载或预览。 */
    DownloadableKnowledgeDocument loadKnowledgeDocument(Long documentId);

    /** 在当前已挂载知识库中执行检索测试。 */
    List<Map<String, Object>> testRetrieval(String query, int topK);

    /** 构造客服运行时的向量元数据过滤表达式。 */
    String resolveMountedKnowledgeFilter();

    /** 查询客服工单。 */
    List<Map<String, Object>> listSessions(String keyword, String status);

    /** 查询某个客服工单的消息明细。 */
    List<Map<String, Object>> getSessionMessages(String sessionId);

    /** 更新工单状态或管理员备注。 */
    void updateSession(String sessionId, Map<String, Object> body, Integer operatorId);

    /** 查询问题提示库。 */
    List<Map<String, Object>> listQuestions(String keyword, String reviewStatus, Boolean enabled);

    /** 新增管理员问题。 */
    void createQuestion(Map<String, Object> body, Integer operatorId);

    /** 修改问题文本、审核状态、启停或备注。 */
    void updateQuestion(Long id, Map<String, Object> body, Integer operatorId);

    /** 删除问题提示。 */
    void deleteQuestion(Long id, Integer operatorId);

    /** 将多个重复问题合并到保留记录。 */
    void mergeQuestions(Long targetId, List<Long> sourceIds, Integer operatorId);

    /** 用户提交客服回答反馈。 */
    void createFeedback(String sessionId, Long messageId, Integer userId, boolean helpful, String reason);

    /** 查询客服回答反馈。 */
    List<Map<String, Object>> listFeedback(Boolean helpful);

    /** 查询没有命中问题提示库的真实用户问题。 */
    List<Map<String, Object>> listUnmatchedQuestions();

    /** 查询智能客服后台操作审计。 */
    List<Map<String, Object>> listOperationLogs();

    /** 知识库下载结果。 */
    record DownloadableKnowledgeDocument(Resource resource, String fileName, long fileSize) {
    }
}
