package cn.wyq.serverwebsocket.utils;

public class BaseContext {
    // 线程容器，每个线程单独存一份，互不干扰
    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    public static Long getCurrentId() {
        return threadLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
    }
}