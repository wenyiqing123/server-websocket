package cn.wyq.serverwebsocket.constant;

/**
 * @author wyq
 * @description Redis Key 通用常量类
 * 规范：建议使用冒号 : 分割层级，例如 "业务模块:子模块:具体含义"
 */
public class RedisKeyConstants {

    /**
     * AI 对话历史记录的缓存名称 (用于 @Cacheable 的 value)
     * 结构: history_conversations::userName:{username}
     */
    public static final String HISTORY_CONVERSATIONS_CACHE = "history_conversations";
    public static final String CONVERSATION_MESSAGES_CACHE = "conversation_messages";
    public static final String MANAGE_MESSAGES_CACHE = "manage_messages";
    public static final String MANAGE_USERS_CACHE = "manage_users";
//    public static final String CONVERSATION_MESSAGES_CACHE = "conversation_messages";

    /**
     * AI 对话历史记录的 Key 前缀
     */
    public static final String HISTORY_CONVERSATIONS_PREFIX = "userName:";

    /**
     * 用户登录 Token 令牌前缀
     * 示例: login:token:{userId}
     */
    public static final String LOGIN_USER_TOKEN = "login:token:";

    /**
     * 图片验证码 Key 前缀
     * 示例: captcha:code:{uuid}
     */
    public static final String CAPTCHA_CODE_KEY = "captcha:code:";

    /**
     * 在线用户列表 (如果需要存 Redis)
     */
    public static final String ONLINE_USER_LIST = "online:users";

    /**
     * 每日活跃用户统计 Key
     * 示例: stats:daily:active:{date}
     */
    public static final String STATS_DAILY_ACTIVE = "stats:daily:active:";

    // 防止被实例化
    private RedisKeyConstants() {}
}