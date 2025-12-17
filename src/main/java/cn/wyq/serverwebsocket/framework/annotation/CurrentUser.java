package cn.wyq.serverwebsocket.framework.annotation;

import java.lang.annotation.*;

/**
 * 用于标识方法参数的注解，表示该参数为当前用户。
 * 在运行时保留注解信息。
 */
@Target({ElementType.PARAMETER})
// 注解的作用目标为方法参数
@Retention(RetentionPolicy.RUNTIME)
// 注解在运行时可通过反射获取
@Documented
// 注解将包含在JavaDoc中
public @interface CurrentUser {
}
