package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.annotation.CurrentUser;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.exception.ServiceException;
import cn.wyq.serverwebsocket.mapper.UserMapper;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;
import cn.wyq.serverwebsocket.service.AiServiceManagementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 智能客服运营管理接口。
 *
 * <p>公开配置和用户反馈只要求登录；其余接口会再次从数据库校验管理员身份，
 * 不依赖前端路由或 Token 中可能过期的角色字段。</p>
 */
@RestController
@RequestMapping("/ai-service")
@RequiredArgsConstructor
@Tag(name = "智能客服管理")
public class AiServiceManagementController {

    private final AiServiceManagementService managementService;
    private final UserMapper userMapper;

    /** 获取用户端客服名称、头像、欢迎语和开关。 */
    @GetMapping("/settings/public")
    public Result publicSettings(@CurrentUser UserEntity currentUser) {
        requireUser(currentUser);
        return Result.success(managementService.getPublicSettings());
    }

    /** 用户对某条客服回答提交有用或无用反馈。 */
    @PostMapping("/feedback")
    public Result createFeedback(@RequestBody Map<String, Object> body,
                                 @CurrentUser UserEntity currentUser) {
        UserEntity user = requireUser(currentUser);
        String sessionId = body == null ? "" : String.valueOf(body.getOrDefault("sessionId", ""));
        Long messageId = body == null || body.get("messageId") == null
                ? null : Long.valueOf(String.valueOf(body.get("messageId")));
        boolean helpful = body != null && (Boolean.TRUE.equals(body.get("helpful"))
                || "1".equals(String.valueOf(body.get("helpful")))
                || "true".equalsIgnoreCase(String.valueOf(body.get("helpful"))));
        String reason = body == null ? "" : String.valueOf(body.getOrDefault("reason", ""));
        managementService.createFeedback(sessionId, messageId, user.getId(), helpful, reason);
        return Result.success("反馈已提交");
    }

    /** 获取后台运行概览。 */
    @GetMapping("/admin/overview")
    public Result overview(@CurrentUser UserEntity currentUser) {
        assertAdmin(currentUser);
        return Result.success(managementService.getOverview());
    }

    /** 获取完整客服配置。 */
    @GetMapping("/admin/settings")
    public Result adminSettings(@CurrentUser UserEntity currentUser) {
        assertAdmin(currentUser);
        return Result.success(managementService.getAdminSettings());
    }

    /** 保存客服配置。 */
    @PutMapping("/admin/settings")
    public Result updateSettings(@RequestBody Map<String, String> body,
                                 @CurrentUser UserEntity currentUser) {
        UserEntity admin = assertAdmin(currentUser);
        managementService.updateSettings(body, admin.getId());
        return Result.success("客服配置已保存");
    }

    /** 查询公共知识库及挂载状态。 */
    @GetMapping("/admin/knowledge-bases")
    public Result listKnowledgeBases(@CurrentUser UserEntity currentUser) {
        assertAdmin(currentUser);
        return Result.success(managementService.listKnowledgeBases());
    }

    /** 新建公共知识库。 */
    @PostMapping("/admin/knowledge-bases")
    public Result createKnowledgeBase(@RequestBody Map<String, Object> body,
                                      @CurrentUser UserEntity currentUser) {
        UserEntity admin = assertAdmin(currentUser);
        return Result.success(managementService.createKnowledgeBase(body, admin.getId()));
    }

    /** 修改公共知识库。 */
    @PutMapping("/admin/knowledge-bases/{id}")
    public Result updateKnowledgeBase(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                      @CurrentUser UserEntity currentUser) {
        UserEntity admin = assertAdmin(currentUser);
        managementService.updateKnowledgeBase(id, body, admin.getId());
        return Result.success("知识库已更新");
    }

    /** 删除空公共知识库。 */
    @DeleteMapping("/admin/knowledge-bases/{id}")
    public Result deleteKnowledgeBase(@PathVariable Long id, @CurrentUser UserEntity currentUser) {
        UserEntity admin = assertAdmin(currentUser);
        managementService.deleteKnowledgeBase(id, admin.getId());
        return Result.success("知识库已删除");
    }

    /** 挂载或卸载客服知识库。 */
    @PutMapping("/admin/knowledge-bases/{id}/binding")
    public Result bindKnowledgeBase(@PathVariable Long id, @RequestParam boolean mounted,
                                    @CurrentUser UserEntity currentUser) {
        UserEntity admin = assertAdmin(currentUser);
        managementService.setKnowledgeBaseMounted(id, mounted, admin.getId());
        return Result.success(mounted ? "知识库已挂载" : "知识库已卸载");
    }

    /** 查询知识库文档。 */
    @GetMapping("/admin/knowledge-bases/{id}/documents")
    public Result listDocuments(@PathVariable Long id, @CurrentUser UserEntity currentUser) {
        assertAdmin(currentUser);
        return Result.success(managementService.listKnowledgeDocuments(id));
    }

    /** 上传并立即构建公共知识库索引。 */
    @PostMapping("/admin/knowledge-bases/{id}/documents")
    public Result uploadDocument(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                                 @CurrentUser UserEntity currentUser) {
        UserEntity admin = assertAdmin(currentUser);
        return Result.success("知识库文档已发布",
                managementService.uploadKnowledgeDocument(id, file, admin.getId()));
    }

    /** 修改文档展示名称。 */
    @PutMapping("/admin/knowledge-documents/{id}/name")
    public Result renameDocument(@PathVariable Long id, @RequestBody Map<String, String> body,
                                 @CurrentUser UserEntity currentUser) {
        UserEntity admin = assertAdmin(currentUser);
        managementService.renameKnowledgeDocument(id, body == null ? "" : body.get("fileName"), admin.getId());
        return Result.success("文档名称已更新");
    }

    /** 重新解析并发布文档索引。 */
    @PostMapping("/admin/knowledge-documents/{id}/reindex")
    public Result reindexDocument(@PathVariable Long id, @CurrentUser UserEntity currentUser) {
        UserEntity admin = assertAdmin(currentUser);
        managementService.reindexKnowledgeDocument(id, admin.getId());
        return Result.success("文档索引已重新发布");
    }

    /** 删除知识库文档。 */
    @DeleteMapping("/admin/knowledge-documents/{id}")
    public Result deleteDocument(@PathVariable Long id, @CurrentUser UserEntity currentUser) {
        UserEntity admin = assertAdmin(currentUser);
        managementService.deleteKnowledgeDocument(id, admin.getId());
        return Result.success("知识库文档已删除");
    }

    /** 下载或由浏览器预览知识库原始文件。 */
    @GetMapping("/admin/knowledge-documents/{id}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadDocument(
            @PathVariable Long id, @CurrentUser UserEntity currentUser) {
        assertAdmin(currentUser);
        AiServiceManagementService.DownloadableKnowledgeDocument document =
                managementService.loadKnowledgeDocument(id);
        MediaType mediaType = MediaTypeFactory.getMediaType(document.fileName())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(document.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(document.fileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(document.resource());
    }

    /** 测试当前挂载知识库的语义检索效果。 */
    @PostMapping("/admin/retrieval-test")
    public Result retrievalTest(@RequestBody Map<String, Object> body,
                                @CurrentUser UserEntity currentUser) {
        assertAdmin(currentUser);
        String query = body == null ? "" : String.valueOf(body.getOrDefault("query", ""));
        int topK = body == null || body.get("topK") == null ? 6
                : Integer.parseInt(String.valueOf(body.get("topK")));
        return Result.success(managementService.testRetrieval(query, topK));
    }

    /** 查询客服工单。 */
    @GetMapping("/admin/sessions")
    public Result sessions(@RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status,
                           @CurrentUser UserEntity currentUser) {
        assertAdmin(currentUser);
        return Result.success(managementService.listSessions(keyword, status));
    }

    /** 查询工单聊天明细。 */
    @GetMapping("/admin/sessions/{id}/messages")
    public Result sessionMessages(@PathVariable String id, @CurrentUser UserEntity currentUser) {
        assertAdmin(currentUser);
        return Result.success(managementService.getSessionMessages(id));
    }

    /** 更新工单处理状态和备注。 */
    @PutMapping("/admin/sessions/{id}")
    public Result updateSession(@PathVariable String id, @RequestBody Map<String, Object> body,
                                @CurrentUser UserEntity currentUser) {
        UserEntity admin = assertAdmin(currentUser);
        managementService.updateSession(id, body, admin.getId());
        return Result.success("工单已更新");
    }

    /** 查询问题提示库。 */
    @GetMapping("/admin/questions")
    public Result questions(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String reviewStatus,
                            @RequestParam(required = false) Boolean enabled,
                            @CurrentUser UserEntity currentUser) {
        assertAdmin(currentUser);
        return Result.success(managementService.listQuestions(keyword, reviewStatus, enabled));
    }

    /** 新增问题提示。 */
    @PostMapping("/admin/questions")
    public Result createQuestion(@RequestBody Map<String, Object> body,
                                 @CurrentUser UserEntity currentUser) {
        UserEntity admin = assertAdmin(currentUser);
        managementService.createQuestion(body, admin.getId());
        return Result.success("问题已添加并刷新缓存");
    }

    /** 修改问题提示。 */
    @PutMapping("/admin/questions/{id}")
    public Result updateQuestion(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                 @CurrentUser UserEntity currentUser) {
        UserEntity admin = assertAdmin(currentUser);
        managementService.updateQuestion(id, body, admin.getId());
        return Result.success("问题已更新并刷新缓存");
    }

    /** 删除问题提示。 */
    @DeleteMapping("/admin/questions/{id}")
    public Result deleteQuestion(@PathVariable Long id, @CurrentUser UserEntity currentUser) {
        UserEntity admin = assertAdmin(currentUser);
        managementService.deleteQuestion(id, admin.getId());
        return Result.success("问题已删除并刷新缓存");
    }

    /** 合并重复问题并累加使用次数。 */
    @PostMapping("/admin/questions/merge")
    @SuppressWarnings("unchecked")
    public Result mergeQuestions(@RequestBody Map<String, Object> body,
                                 @CurrentUser UserEntity currentUser) {
        UserEntity admin = assertAdmin(currentUser);
        Long targetId = body == null || body.get("targetId") == null ? null
                : Long.valueOf(String.valueOf(body.get("targetId")));
        List<Long> sourceIds = body == null || !(body.get("sourceIds") instanceof List<?> values)
                ? List.of() : values.stream().map(value -> Long.valueOf(String.valueOf(value))).toList();
        managementService.mergeQuestions(targetId, sourceIds, admin.getId());
        return Result.success("重复问题已合并");
    }

    /** 查询用户回答反馈。 */
    @GetMapping("/admin/feedback")
    public Result feedback(@RequestParam(required = false) Boolean helpful,
                           @CurrentUser UserEntity currentUser) {
        assertAdmin(currentUser);
        return Result.success(managementService.listFeedback(helpful));
    }

    /** 查询未命中题库的真实用户问题，供运营补充提示库。 */
    @GetMapping("/admin/unmatched-questions")
    public Result unmatchedQuestions(@CurrentUser UserEntity currentUser) {
        assertAdmin(currentUser);
        return Result.success(managementService.listUnmatchedQuestions());
    }

    /** 查询客服运营审计日志。 */
    @GetMapping("/admin/operation-logs")
    public Result operationLogs(@CurrentUser UserEntity currentUser) {
        assertAdmin(currentUser);
        return Result.success(managementService.listOperationLogs());
    }

    private UserEntity requireUser(UserEntity currentUser) {
        if (currentUser == null || currentUser.getId() <= 0) {
            throw new ServiceException("请先登录", 401);
        }
        return currentUser;
    }

    private UserEntity assertAdmin(UserEntity currentUser) {
        UserEntity user = requireUser(currentUser);
        var persisted = userMapper.getCurrentUserById(user.getId());
        if (persisted == null || !Integer.valueOf(1).equals(persisted.getStatus())
                || (persisted.getRole() != 0 && persisted.getRole() != 1)) {
            throw new ServiceException("仅管理员可以管理智能客服", 403);
        }
        return user;
    }
}
