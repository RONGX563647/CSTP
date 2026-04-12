package com.aisale.backend.util;

import lombok.extern.slf4j.Slf4j;

/**
 * 日志工具类
 * 提供统一的日志记录方法
 */
@Slf4j
public class LogUtils {

    private LogUtils() {
        // 工具类不允许实例化
    }

    /**
     * 记录进入方法的日志
     */
    public static void traceEnter(String className, String methodName, Object... args) {
        if (log.isTraceEnabled()) {
            log.trace(">>> 进入方法：{}.{}，参数：{}", className, methodName, formatArgs(args));
        }
    }

    /**
     * 记录离开方法的日志
     */
    public static void traceExit(String className, String methodName, Object result) {
        if (log.isTraceEnabled()) {
            log.trace("<<< 离开方法：{}.{}，返回值：{}", className, methodName, formatArg(result));
        }
    }

    /**
     * 记录业务操作日志
     */
    public static void logBusinessAction(String action, String detail) {
        log.info("[业务操作] {}: {}", action, detail);
    }

    /**
     * 记录业务操作日志（带操作人）
     */
    public static void logBusinessAction(String action, String detail, String operator) {
        log.info("[业务操作] [操作人：{}] {}: {}", operator, action, detail);
    }

    /**
     * 记录审计日志
     */
    public static void logAudit(String module, String action, String detail) {
        log.info("[审计日志] [模块：{}] {}: {}", module, action, detail);
    }

    /**
     * 记录性能日志
     */
    public static void logPerformance(String operation, long durationMs) {
        if (durationMs > 1000) {
            log.warn("[性能警告] 操作：{} 耗时：{}ms", operation, durationMs);
        } else {
            log.debug("[性能] 操作：{} 耗时：{}ms", operation, durationMs);
        }
    }

    /**
     * 格式化参数
     */
    private static String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "无";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(formatArg(args[i]));
        }
        return sb.toString();
    }

    /**
     * 格式化单个参数
     */
    private static String formatArg(Object arg) {
        if (arg == null) {
            return "null";
        }
        String str = arg.toString();
        if (str.length() > 100) {
            return str.substring(0, 100) + "...";
        }
        return str;
    }

}
