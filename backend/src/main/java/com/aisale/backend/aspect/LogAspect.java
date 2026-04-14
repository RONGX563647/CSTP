package com.aisale.backend.aspect;

import com.aisale.backend.entity.OperationLog;
import com.aisale.backend.entity.User;
import com.aisale.backend.service.OperationLogService;
import com.aisale.backend.util.JwtRequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 日志切面
 * 记录 Controller 和 Service 层方法执行情况
 */
@Slf4j
@Aspect
@Component
public class LogAspect {

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private JwtRequestUtils jwtRequestUtils;

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
            long duration = endTime - startTime;
            log.info("<<< 执行完成：{} | 耗时：{}ms", methodFullName, duration);
            
            // 保存操作日志到数据库
            saveOperationLog(methodFullName, joinPoint.getArgs(), duration, "SUCCESS", null);
            
            return result;
        } catch (Throwable e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            log.error("!!! 执行失败：{} | 耗时：{}ms | 错误：{}", methodFullName, duration, e.getMessage());
            
            // 保存失败日志到数据库
            saveOperationLog(methodFullName, joinPoint.getArgs(), duration, "FAIL", e.getMessage());
            
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

    /**
     * 保存操作日志到数据库
     */
    private void saveOperationLog(String methodFullName, Object[] args, Long duration, String status, String errorMessage) {
        try {
            HttpServletRequest request = getRequest();
            if (request == null) {
                return;
            }

            // 获取当前用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return;
            }

            Long operatorId = null;
            String operatorName = null;
            OperationLog.OperatorRole operatorRole = null;

            // 从认证信息中获取用户ID
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                User user = (User) principal;
                operatorId = user.getId();
                operatorName = user.getUsername();
                operatorRole = OperationLog.OperatorRole.BUYER;
            } else if (principal instanceof org.springframework.security.core.userdetails.User) {
                org.springframework.security.core.userdetails.User userDetails = 
                    (org.springframework.security.core.userdetails.User) principal;
                operatorName = userDetails.getUsername();
                operatorRole = OperationLog.OperatorRole.ADMIN;
            }

            // 解析模块和操作
            String module = parseModule(methodFullName);
            String action = parseAction(methodFullName);
            String description = methodFullName;

            // 保存日志
            operationLogService.logOperation(
                module, 
                action, 
                operatorId, 
                operatorName, 
                operatorRole, 
                description, 
                request, 
                status, 
                errorMessage, 
                duration
            );
        } catch (Exception e) {
            log.error("保存操作日志失败：{}", e.getMessage());
        }
    }

    /**
     * 获取当前请求
     */
    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 解析模块名称
     */
    private String parseModule(String methodFullName) {
        if (methodFullName.contains("Product")) {
            return "PRODUCT";
        } else if (methodFullName.contains("Order")) {
            return "ORDER";
        } else if (methodFullName.contains("User")) {
            return "USER";
        } else if (methodFullName.contains("Address")) {
            return "ADDRESS";
        } else if (methodFullName.contains("Chat")) {
            return "CHAT";
        } else if (methodFullName.contains("Log")) {
            return "LOG";
        } else if (methodFullName.contains("Auth")) {
            return "AUTH";
        } else if (methodFullName.contains("Oss")) {
            return "OSS";
        } else {
            return "SYSTEM";
        }
    }

    /**
     * 解析操作类型
     */
    private String parseAction(String methodFullName) {
        if (methodFullName.contains("create") || methodFullName.contains("add") || methodFullName.contains("new")) {
            return "CREATE";
        } else if (methodFullName.contains("update") || methodFullName.contains("edit")) {
            return "UPDATE";
        } else if (methodFullName.contains("delete") || methodFullName.contains("remove")) {
            return "DELETE";
        } else if (methodFullName.contains("get") || methodFullName.contains("find") || methodFullName.contains("query") || methodFullName.contains("list")) {
            return "QUERY";
        } else if (methodFullName.contains("login")) {
            return "LOGIN";
        } else if (methodFullName.contains("logout")) {
            return "LOGOUT";
        } else if (methodFullName.contains("upload")) {
            return "UPLOAD";
        } else if (methodFullName.contains("send")) {
            return "SEND";
        } else {
            return "OTHER";
        }
    }

}
