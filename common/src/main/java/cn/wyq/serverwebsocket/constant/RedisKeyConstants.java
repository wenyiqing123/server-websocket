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

    public static final String USER_CACHE="user:id:";

}