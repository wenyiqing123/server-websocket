package cn.wyq.serverwebsocket.constant;

/**
 * RedPacketConstant：常量定义类，集中维护系统固定值。
 */
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
    public static final String REDIS_KEY_RECEIVER_SUFFIX = ":receiver";
}
