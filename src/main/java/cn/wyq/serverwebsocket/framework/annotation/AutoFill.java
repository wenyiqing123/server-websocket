package cn.wyq.serverwebsocket.framework.annotation;


import cn.wyq.serverwebsocket.framework.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据库操作类型注解，用于标识某个方法需要根据注解中指定的数据库操作类型进行自动填充
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFill {
    OperationType value();
}
