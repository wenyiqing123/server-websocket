package cn.wyq.serverwebsocket.aspect;


import cn.wyq.serverwebsocket.annotation.AutoFill;
import cn.wyq.serverwebsocket.constant.AutoFillConstant;
import cn.wyq.serverwebsocket.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 切面类，用于处理自动填充注解
 */
@Component
@Aspect
@Slf4j
public class AutoFillAspect {
    /**
     * 切入点，匹配所有被AutoFill注解标注的方法
     */
    @Pointcut("execution(* cn.wyq.serverwebsocket.mapper.*.*(..)) && @annotation(cn.wyq.serverwebsocket.annotation.AutoFill)")    public void autoFillPointCut() {
    }

    /**
     * 前置通知，在被AutoFill注解标注的方法执行前调用
     * @param joinPoint
     */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) {
        log.info("开始进行公共字段自动填充...");
        //获得到当前被拦截的方法上的数据库操作实现
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();//方法签名对象
        AutoFill autoFill = methodSignature.getMethod().getAnnotation(AutoFill.class);//获得方法上的注解对象
        OperationType operationType = autoFill.value();//获得注解上的数据库操作类型
        //获得当前被拦截方法的参数--实体对象
        Object[] args = joinPoint.getArgs();
        if (args.length==0) return;
        //获得实体对象
        Object entity = args[0];
        //准备赋值的数据
        LocalDateTime now = LocalDateTime.now();
//        Long currentId = BaseContext.getCurrentId();
        Long currentId = 1L;
        //根据数据库操作类型，通过反射为实体对象的公共字段赋值
        if (operationType== OperationType.INSERT){
            //为四个字段插入赋值
           try {
               Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
               Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
               Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
               Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
               setCreateTime.invoke(entity, now);
               setCreateUser.invoke(entity, currentId);
               setUpdateTime.invoke(entity, now);
               setUpdateUser.invoke(entity, currentId);
           } catch (Exception e) {
               log.error("插入操作公共字段自动填充失败", e);
           }
        }else if (operationType==OperationType.UPDATE){
            //为更新操作赋值
            try {
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, currentId);
            } catch (Exception e) {
                log.error("更新操作公共字段自动填充失败", e);
            }
        }
        log.info("公共字段自动填充结束，操作类型：{}", operationType);
    }
}
