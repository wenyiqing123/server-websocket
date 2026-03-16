package cn.wyq.serverwebsocket.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
// 注解的作用目标为方法参数
@Retention(RetentionPolicy.RUNTIME)
@Documented
// 注解将包含在JavaDoc中
public @interface LogOut {
}
