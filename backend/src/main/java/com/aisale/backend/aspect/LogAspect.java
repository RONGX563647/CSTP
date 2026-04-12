package com.aisale.backend.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 日志切面
 * 记录 Controller 和 Service 层方法执行情况
 */
@Slf4j
@Aspect
@Component
public class LogAspect {

    /**
     * Controller 层切点
     */
    @Pointcut("execution(* com.aisale.backend.controller..*.*(..))")
    public void controllerPointcut() {
    }

    /**
     * Service 层切点
     */
    @Pointcut("execution(* com.aisale.backend.service..*.*(..))")
    public void servicePointcut() {
    }

    /**
     * 环绕通知 - 记录方法执行
     */
    @Around("controllerPointcut() || servicePointcut()")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        String methodFullName = className + "." + methodName;

        // 判断是否是 Service 层方法
        boolean isService = className.contains(".service.");

        // Service 层只记录 DEBUG 日志
        if (isService) {
            log.debug("开始执行：{}", methodFullName);
            long startTime = System.currentTimeMillis();
            try {
                Object result = joinPoint.proceed();
                long endTime = System.currentTimeMillis();
                log.debug("执行完成：{} | 耗时：{}ms", methodFullName, (endTime - startTime));
                return result;
            } catch (Throwable e) {
                long endTime = System.currentTimeMillis();
                log.error("执行失败：{} | 耗时：{}ms | 错误：{}", methodFullName, (endTime - startTime), e.getMessage());
                throw e;
            }
        }

        // Controller 层记录 INFO 日志
        log.info(">>> 开始执行：{}", methodFullName);
        logParams(joinPoint.getArgs());

        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();
            log.info("<<< 执行完成：{} | 耗时：{}ms", methodFullName, (endTime - startTime));
            return result;
        } catch (Throwable e) {
            long endTime = System.currentTimeMillis();
            log.error("!!! 执行失败：{} | 耗时：{}ms | 错误：{}", methodFullName, (endTime - startTime), e.getMessage());
            throw e;
        }
    }

    /**
     * 记录方法参数
     */
    private void logParams(Object[] args) {
        if (args != null && args.length > 0) {
            StringBuilder sb = new StringBuilder("参数：");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(formatArg(args[i]));
            }
            log.info(sb.toString());
        }
    }

    /**
     * 格式化参数
     */
    private String formatArg(Object arg) {
        if (arg == null) {
            return "null";
        }

        String argStr = arg.toString();
        // 限制参数长度
        if (argStr.length() > 200) {
            return argStr.substring(0, 200) + "...";
        }
        return argStr;
    }

}
