package cn.wyq.serverwebsocket.common;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring 工具类
 * <p>
 * 作用：
 * 1. 在非 Spring 管理的类中获取 Spring Bean
 * 例如：WebSocket ServerEndpoint、普通工具类等
 * 2. 提供静态方法直接获取 ApplicationContext 或 Bean
 */
@Component
public class ApplicationHelper implements ApplicationContextAware {

    // Spring 上下文对象，静态保存，方便全局使用
    private static ApplicationContext applicationContext;

    /**
     * 获取 Spring 上下文对象
     *
     * @return ApplicationContext
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Spring 在启动时会自动调用此方法，将 ApplicationContext 注入
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationHelper.applicationContext = applicationContext;
    }

    /**
     * 根据 Bean 名称获取 Spring 管理的 Bean
     *
     * @param beanName Bean 名称
     * @return 对应的 Bean 对象
     */
    public static Object getBean(String beanName) {
        return applicationContext.getBean(beanName);
    }

    /**
     * 可选：根据类型获取 Bean
     */
    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }
}
