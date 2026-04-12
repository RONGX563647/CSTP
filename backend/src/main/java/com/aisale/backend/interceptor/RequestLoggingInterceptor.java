package com.aisale.backend.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 请求日志拦截器
 * 记录所有 HTTP 请求的详细信息
 */
@Slf4j
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTRIBUTE = "startTime";

    /**
     * 请求处理前
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME_ATTRIBUTE, startTime);

        log.info(">>> [{}] {} {}",
                request.getMethod(),
                request.getRequestURI(),
                formatQueryString(request.getQueryString()));

        return true;
    }

    /**
     * 请求处理后
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) {
        long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
        long duration = System.currentTimeMillis() - startTime;

        log.info("<<< [{}] {} - Status: {} | Duration: {}ms",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration);
    }

    /**
     * 请求完成后（视图渲染后）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        // 可以在这里记录最终的完成情况
        if (ex != null) {
            log.error("!!! [{}] {} - Exception: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    ex.getMessage());
        }
    }

    /**
     * 格式化查询字符串
     */
    private String formatQueryString(String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return "";
        }
        return "?" + queryString;
    }

}
