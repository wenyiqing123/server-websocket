package cn.wyq.serverwebsocket.annotation;

import java.lang.annotation.*;

/**
 * 自定义注解，用于标记无需进行令牌验证的路径。
 */
/**
 * LoginNotRequired：自定义注解，用于声明通用的业务元数据。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LoginNotRequired {
}
