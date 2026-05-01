package cn.wyq.serverwebsocket.constant;

public class RedPacketConstant {
    /**
     * Redis 红包业务全局前缀
     */
    public static final String REDIS_KEY_PREFIX = "chat:redpacket:";

    /**
     * Redis 红包金额队列后缀
     */
    public static final String REDIS_KEY_LIST_SUFFIX = ":list";

    /**
     * Redis 红包剩余个数后缀
     */
    public static final String REDIS_KEY_COUNT_SUFFIX = ":count";
    public static final String REDIS_KEY_GRABBED_SET_SUFFIX = ":grabbed";
}
