package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.annotation.CurrentUser;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.exception.ServiceException;
import cn.wyq.serverwebsocket.mapper.UserMapper;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;
import cn.wyq.serverwebsocket.service.SysConfigService;
import cn.wyq.serverwebsocket.socket.WebSocket;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * SysConfigController：Web 层控制器，负责接收并处理 HTTP 请求。
 */
@RestController
@RequestMapping("/sys-config")
@Tag(name = "系统设置接口")
public class SysConfigController {

    /** 云端业务统一使用的模型；本地 Ollama 标签提取不经过此配置。 */
    private static final String REMOTE_CHAT_MODEL = "qwen3.8-max-0902";

    /** 仅允许出现在平台设置页的配置键，避免任意键写入配置表。 */
    private static final Set<String> ALLOWED_KEYS = new HashSet<>(Arrays.asList(
            "theme_color", "system_language", "site_name", "default_home", "timezone", "date_format",
            "registration_enabled", "maintenance_mode", "maintenance_message",
            "ai_default_model", "ai_fallback_model", "ai_model_whitelist", "ai_request_timeout_ms",
            "ai_retry_count", "ai_max_tokens", "ai_concurrency", "ai_rate_limit", "ai_stream_enabled",
            "rag_top_k", "rag_similarity_threshold", "rag_chunk_size", "rag_chunk_overlap",
            "rag_max_context_chars", "rag_max_slices", "rag_index_refresh_strategy", "rag_max_upload_bytes",
            "rag_allowed_file_types", "chat_max_message_length", "chat_max_sessions_per_user",
            "chat_history_retention_days", "websocket_heartbeat_seconds", "ai_error_message",
            "sensitive_words_enabled", "file_max_upload_bytes", "file_allowed_types",
            "file_duplicate_strategy", "file_temp_cleanup_hours", "document_retention_days",
            "jwt_access_token_minutes", "jwt_refresh_token_days", "login_failure_limit",
            "password_min_length", "operation_log_retention_days", "system_announcement",
            "email_enabled", "websocket_notification_enabled", "alert_enabled", "transfer_expire_hours",
            "red_packet_expire_hours", "recharge_close_minutes",
            // 兼容旧版女友设置页，新的管理入口使用 /ai/admin/builtin/girlfriend。
            "ai_system_prompt", "ai_temperature", "ai_avatar", "ai_theme_color",
            "ai_girlfriend_name", "ai_girlfriend_avatar", "ai_girlfriend_welcome",
            "ai_girlfriend_system_prompt", "ai_girlfriend_model", "ai_girlfriend_temperature",
            "ai_girlfriend_max_tokens", "ai_girlfriend_theme_color", "ai_girlfriend_status"
    ));

    private static final Set<String> INTEGER_KEYS = new HashSet<>(Arrays.asList(
            "ai_request_timeout_ms", "ai_retry_count", "ai_max_tokens", "ai_concurrency", "ai_rate_limit",
            "rag_top_k", "rag_chunk_size", "rag_chunk_overlap", "rag_max_context_chars", "rag_max_slices",
            "rag_max_upload_bytes", "chat_max_message_length", "chat_max_sessions_per_user",
            "chat_history_retention_days", "websocket_heartbeat_seconds", "file_max_upload_bytes",
            "file_temp_cleanup_hours", "document_retention_days", "jwt_access_token_minutes",
            "jwt_refresh_token_days", "login_failure_limit", "password_min_length",
            "operation_log_retention_days", "transfer_expire_hours", "red_packet_expire_hours",
            "recharge_close_minutes", "ai_girlfriend_max_tokens", "ai_girlfriend_status"
    ));

    private static final Set<String> DECIMAL_KEYS = new HashSet<>(Arrays.asList(
            "rag_similarity_threshold", "ai_temperature", "ai_girlfriend_temperature"
    ));

    private static final Set<String> BOOLEAN_KEYS = new HashSet<>(Arrays.asList(
            "registration_enabled", "maintenance_mode", "ai_stream_enabled", "sensitive_words_enabled",
            "email_enabled", "websocket_notification_enabled", "alert_enabled"
    ));

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取所有系统配置
     * 前端收到的是类似于 { "theme_color": "#409EFF", "ai_top_k": "3" } 这样的纯键值对对象
     */
    @GetMapping("/getAll")
    public Result getAllConfigs(@CurrentUser UserEntity currentUser) {
        requireCurrentUser(currentUser);
        return Result.success(sysConfigService.getConfigMap());
    }

    /**
     * 获取供用户端展示的系统公告。
     *
     * <p>用户端不能读取完整的系统配置，因此这里仅返回已经发布的公告文本。</p>
     */
    @GetMapping("/announcement")
    public Result getAnnouncement(@CurrentUser UserEntity currentUser) {
        requireCurrentUser(currentUser);
        String announcement = sysConfigService.getConfigMap().get("system_announcement");
        return Result.success(Collections.singletonMap("content",
                announcement == null ? "" : announcement));
    }

    /**
     * 批量保存系统配置
     * 前端传过来一个 JSON 对象 (Map)
     */
    @PostMapping("/updateBatch")
    public Result updateBatch(@RequestBody Map<String, String> configMap,
                              @CurrentUser UserEntity currentUser) {
        assertAdmin(currentUser);
        if (configMap == null || configMap.isEmpty()) {
            throw new ServiceException("配置内容不能为空", 400);
        }
        Map<String, String> validated = new HashMap<>();
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().trim();
            if (!ALLOWED_KEYS.contains(key)) {
                throw new ServiceException("不支持修改配置项：" + key, 400);
            }
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            validateValue(key, value);
            validated.put(key, value);
        }
        validateCrossFieldValues(validated);
        String previousAnnouncement = sysConfigService.getConfigMap()
                .getOrDefault("system_announcement", "");
        boolean announcementChanged = validated.containsKey("system_announcement")
                && !Objects.equals(previousAnnouncement, validated.get("system_announcement"));
        sysConfigService.updateValues(validated);

        // 配置保存成功后同步刷新在线用户的公告；离线用户会在进入聊天页时通过公告接口获取最新内容。
        if (announcementChanged) {
            WebSocket.broadcastSystemAnnouncement(validated.get("system_announcement"));
        }
        return Result.success("保存成功");
    }

    private UserEntity requireCurrentUser(UserEntity currentUser) {
        if (currentUser == null || currentUser.getId() <= 0) {
            throw new ServiceException("请先登录", 401);
        }
        return currentUser;
    }

    private void assertAdmin(UserEntity currentUser) {
        UserEntity user = requireCurrentUser(currentUser);
        var persisted = userMapper.getCurrentUserById(user.getId());
        if (persisted == null || !Integer.valueOf(1).equals(persisted.getStatus())
                || (persisted.getRole() != 0 && persisted.getRole() != 1)) {
            throw new ServiceException("仅管理员可以修改系统设置", 403);
        }
    }

    private void validateValue(String key, String value) {
        if (value.length() > maxLength(key)) {
            throw new ServiceException("配置项 " + key + " 的长度超出限制", 400);
        }
        if (INTEGER_KEYS.contains(key)) {
            try {
                long number = Long.parseLong(value);
                if (number < integerMin(key) || number > integerMax(key)) {
                    throw new ServiceException("配置项 " + key + " 的数值范围不正确", 400);
                }
            } catch (NumberFormatException e) {
                throw new ServiceException("配置项 " + key + " 必须是整数", 400);
            }
        } else if (DECIMAL_KEYS.contains(key)) {
            try {
                BigDecimal number = new BigDecimal(value);
                if (number.compareTo(BigDecimal.ZERO) < 0 || number.compareTo(BigDecimal.ONE) > 0) {
                    throw new ServiceException("配置项 " + key + " 必须在 0 到 1 之间", 400);
                }
            } catch (NumberFormatException e) {
                throw new ServiceException("配置项 " + key + " 必须是数字", 400);
            }
        } else if (BOOLEAN_KEYS.contains(key)
                && !("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)
                || "0".equals(value) || "1".equals(value))) {
            throw new ServiceException("配置项 " + key + " 必须是布尔值", 400);
        } else if ("theme_color".equals(key)
                && !("light".equalsIgnoreCase(value) || "dark".equalsIgnoreCase(value))) {
            throw new ServiceException("系统主题只能是 light 或 dark", 400);
        } else if ("system_language".equals(key)
                && !("zh-CN".equalsIgnoreCase(value) || "en-US".equalsIgnoreCase(value))) {
            throw new ServiceException("系统语言只能是 zh-CN 或 en-US", 400);
        } else if ("ai_default_model".equals(key) || "ai_girlfriend_model".equals(key)) {
            validateRemoteModel(key, value, false);
        } else if ("ai_fallback_model".equals(key)) {
            validateRemoteModel(key, value, true);
        } else if ("ai_model_whitelist".equals(key)) {
            validateRemoteModelWhitelist(value);
        } else if ("rag_allowed_file_types".equals(key)) {
            validateAllowedDocumentTypes(value);
        } else if (("ai_avatar".equals(key) || "ai_girlfriend_avatar".equals(key))
                && !value.isEmpty() && !value.matches("^(https?://|/|data:|blob:).+")) {
            throw new ServiceException("头像地址格式不正确", 400);
        } else if (("ai_theme_color".equals(key) || "ai_girlfriend_theme_color".equals(key))
                && !value.matches("^#[0-9a-fA-F]{6}$")) {
            throw new ServiceException("主题色必须是六位十六进制颜色", 400);
        }
    }

    private void validateRemoteModel(String key, String value, boolean allowBlank) {
        if (allowBlank && value.isBlank()) {
            return;
        }
        if (!REMOTE_CHAT_MODEL.equals(value)) {
            throw new ServiceException("配置项 " + key + " 仅支持 " + REMOTE_CHAT_MODEL
                    + "，Ollama 模型由本地标签提取器单独管理", 400);
        }
    }

    private void validateRemoteModelWhitelist(String value) {
        if (value.isBlank()) {
            throw new ServiceException("模型白名单不能为空", 400);
        }
        for (String model : value.split(",")) {
            if (!REMOTE_CHAT_MODEL.equals(model.trim())) {
                throw new ServiceException("模型白名单仅支持 " + REMOTE_CHAT_MODEL, 400);
            }
        }
    }

    private void validateAllowedDocumentTypes(String value) {
        if (value.isBlank()) {
            throw new ServiceException("知识库文件类型不能为空", 400);
        }
        for (String type : value.split(",")) {
            String normalized = type.trim().toLowerCase(java.util.Locale.ROOT);
            if (normalized.startsWith(".")) {
                normalized = normalized.substring(1);
            }
            if (!("pdf".equals(normalized) || "txt".equals(normalized) || "md".equals(normalized))) {
                throw new ServiceException("当前文档解析器仅支持 pdf、txt、md 格式", 400);
            }
        }
    }

    /** 校验只提交一个 RAG 参数时与数据库中另一个参数的组合关系。 */
    private void validateCrossFieldValues(Map<String, String> submitted) {
        if (!submitted.containsKey("rag_chunk_size") && !submitted.containsKey("rag_chunk_overlap")) {
            return;
        }
        Map<String, String> current = sysConfigService.getConfigMap();
        String chunkSizeText = submitted.containsKey("rag_chunk_size")
                ? submitted.get("rag_chunk_size") : current.getOrDefault("rag_chunk_size", "800");
        String overlapText = submitted.containsKey("rag_chunk_overlap")
                ? submitted.get("rag_chunk_overlap") : current.getOrDefault("rag_chunk_overlap", "120");
        try {
            long chunkSize = Long.parseLong(chunkSizeText);
            long overlap = Long.parseLong(overlapText);
            if (overlap >= chunkSize) {
                throw new ServiceException("分块重叠长度必须小于分块大小", 400);
            }
        } catch (NumberFormatException exception) {
            throw new ServiceException("RAG 分块参数必须是整数", 400);
        }
    }

    private int integerMin(String key) {
        if ("ai_max_tokens".equals(key) || "ai_girlfriend_max_tokens".equals(key)) {
            return 64;
        }
        if ("ai_request_timeout_ms".equals(key)) {
            return 1_000;
        }
        if ("ai_concurrency".equals(key) || "ai_rate_limit".equals(key)
                || "rag_top_k".equals(key) || "chat_max_sessions_per_user".equals(key)
                || "chat_history_retention_days".equals(key) || "websocket_heartbeat_seconds".equals(key)
                || "file_temp_cleanup_hours".equals(key) || "document_retention_days".equals(key)
                || "jwt_access_token_minutes".equals(key) || "jwt_refresh_token_days".equals(key)
                || "login_failure_limit".equals(key) || "password_min_length".equals(key)
                || "operation_log_retention_days".equals(key) || "transfer_expire_hours".equals(key)
                || "red_packet_expire_hours".equals(key) || "recharge_close_minutes".equals(key)) {
            return 1;
        }
        if ("rag_chunk_size".equals(key)) {
            return 100;
        }
        if ("rag_max_context_chars".equals(key) || "chat_max_message_length".equals(key)) {
            return 100;
        }
        if ("rag_max_slices".equals(key)) {
            return 1;
        }
        if ("rag_max_upload_bytes".equals(key) || "file_max_upload_bytes".equals(key)) {
            return 1_024;
        }
        return 0;
    }

    private int integerMax(String key) {
        if ("ai_request_timeout_ms".equals(key)) {
            return 3_600_000;
        }
        if ("rag_max_upload_bytes".equals(key) || "file_max_upload_bytes".equals(key)) {
            return 1_073_741_824;
        }
        if ("rag_max_context_chars".equals(key) || "chat_max_message_length".equals(key)) {
            return 1_000_000;
        }
        if ("rag_max_slices".equals(key)) {
            return 10_000;
        }
        if ("ai_max_tokens".equals(key) || "ai_girlfriend_max_tokens".equals(key)) {
            return 8_192;
        }
        if ("ai_retry_count".equals(key)) {
            return 10;
        }
        if ("ai_concurrency".equals(key)) {
            return 1_000;
        }
        if ("ai_rate_limit".equals(key)) {
            return 100_000;
        }
        if ("rag_top_k".equals(key)) {
            return 50;
        }
        if ("rag_chunk_size".equals(key)) {
            return 10_000;
        }
        if ("rag_chunk_overlap".equals(key)) {
            return 2_000;
        }
        if ("ai_girlfriend_status".equals(key)) {
            return 1;
        }
        if (key.contains("timeout") || key.contains("retention") || key.contains("expire")
                || key.contains("cleanup") || key.contains("heartbeat") || key.contains("minutes")
                || key.contains("days") || key.contains("hours")) {
            return 365 * 24 * 60;
        }
        return 10_000_000;
    }

    private int maxLength(String key) {
        if (key.contains("prompt") || "system_announcement".equals(key) || "ai_error_message".equals(key)) {
            return 20_000;
        }
        if (key.contains("whitelist") || key.contains("allowed_types") || key.contains("message")) {
            return 2_000;
        }
        return 500;
    }
}
