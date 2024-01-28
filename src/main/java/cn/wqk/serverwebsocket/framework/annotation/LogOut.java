package cn.wqk.serverwebsocket.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
// 注解的作用目标为方法参数
@Retention(RetentionPolicy.RUNTIME)
public @interface LogOut {
}
