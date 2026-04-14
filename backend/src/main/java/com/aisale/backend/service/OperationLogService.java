package com.aisale.backend.service;

import com.aisale.backend.dto.LogQueryRequest;
import com.aisale.backend.entity.OperationLog;
import com.aisale.backend.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 操作日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    /**
     * 记录操作日志
     */
    public void saveLog(OperationLog operationLog) {
        operationLogRepository.save(operationLog);
    }

    /**
     * 创建并保存操作日志
     */
    public void logOperation(String module, String action, Long operatorId, String operatorName,
                            OperationLog.OperatorRole operatorRole, String description,
                            HttpServletRequest request, String status, String message, Long duration) {
        OperationLog operationLog = new OperationLog();
        operationLog.setModule(module);
        operationLog.setAction(action);
        operationLog.setOperatorId(operatorId);
        operationLog.setOperatorName(operatorName);
        operationLog.setOperatorRole(operatorRole);
        operationLog.setDescription(description);
        operationLog.setRequestMethod(request != null ? request.getMethod() : null);
        operationLog.setRequestUrl(request != null ? request.getRequestURI() : null);
        operationLog.setRequestParams(request != null ? buildRequestParams(request) : null);
        operationLog.setStatus(status != null ? status : "SUCCESS");
        operationLog.setMessage(message);
        operationLog.setDuration(duration);
        operationLog.setIp(request != null ? getClientIp(request) : null);
        operationLog.setUserAgent(request != null ? request.getHeader("User-Agent") : null);
        
        operationLogRepository.save(operationLog);
    }

    /**
     * 记录成功操作
     */
    public void logSuccess(String module, String action, Long operatorId, String operatorName,
                          OperationLog.OperatorRole operatorRole, String description,
                          HttpServletRequest request, Long duration) {
        logOperation(module, action, operatorId, operatorName, operatorRole, 
                   description, request, "SUCCESS", null, duration);
    }

    /**
     * 记录失败操作
     */
    public void logFail(String module, String action, Long operatorId, String operatorName,
                       OperationLog.OperatorRole operatorRole, String description,
                       HttpServletRequest request, String errorMessage, Long duration) {
        logOperation(module, action, operatorId, operatorName, operatorRole, 
                   description, request, "FAIL", errorMessage, duration);
    }

    /**
     * 记录系统操作（无用户上下文）
     */
    public void logSystem(String module, String action, String description, String status, String message) {
        OperationLog operationLog = new OperationLog();
        operationLog.setModule(module);
        operationLog.setAction(action);
        operationLog.setOperatorRole(OperationLog.OperatorRole.SYSTEM);
        operationLog.setDescription(description);
        operationLog.setStatus(status);
        operationLog.setMessage(message);
        operationLogRepository.save(operationLog);
    }

    /**
     * 分页查询日志
     */
    public Page<OperationLog> queryLogs(LogQueryRequest request) {
        Pageable pageable = PageRequest.of(
            request.getPage(),
            request.getSize(),
            Sort.by(Sort.Direction.DESC, "createTime")
        );

        Specification<OperationLog> spec = buildSpecification(request);
        return operationLogRepository.findAll(spec, pageable);
    }

    /**
     * 根据ID查询日志
     */
    public OperationLog getLogById(Long id) {
        return operationLogRepository.findById(id).orElse(null);
    }

    /**
     * 获取最近日志
     */
    public List<OperationLog> getRecentLogs(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return operationLogRepository.findAll(
            Sort.by(Sort.Direction.DESC, "createTime"), 
            pageable
        ).getContent();
    }

    /**
     * 获取日志统计
     */
    public LogStats getLogStats() {
        LogStats stats = new LogStats();
        
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime startOfWeek = today.minusDays(7);
        
        stats.setTodayCount(operationLogRepository.countTodayLogs(today));
        stats.setWeekCount(operationLogRepository.countByTimeRange(startOfWeek, LocalDateTime.now()));
        stats.setTotalCount(operationLogRepository.count());
        
        // 获取各模块统计
        List<Object[]> moduleStats = operationLogRepository.countLogsByModule(startOfWeek, LocalDateTime.now());
        stats.setModuleStats(moduleStats.stream()
            .limit(10)
            .collect(java.util.stream.Collectors.toMap(
                arr -> (String) arr[0],
                arr -> ((Number) arr[1]).longValue()
            )));
        
        // 获取各状态统计
        List<Object[]> statusStats = operationLogRepository.countLogsByStatus(startOfWeek, LocalDateTime.now());
        stats.setStatusStats(statusStats.stream()
            .collect(java.util.stream.Collectors.toMap(
                arr -> (String) arr[0],
                arr -> ((Number) arr[1]).longValue()
            )));
        
        return stats;
    }

    /**
     * 删除指定时间之前的日志（数据清理）
     */
    public long deleteOldLogs(LocalDateTime before) {
        // 先查询要删除的数量
        LocalDateTime now = LocalDateTime.now();
        long count = operationLogRepository.countByTimeRange(LocalDateTime.of(2000, 1, 1, 0, 0), before);
        
        // 使用原生查询删除
        if (count > 0) {
            operationLogRepository.deleteAllInBatch(
                operationLogRepository.findAll(
                    (Specification<OperationLog>) (root, query, cb) -> 
                        cb.lessThan(root.get("createTime"), before)
                )
            );
        }
        return count;
    }

    /**
     * 根据业务信息关联日志
     */
    public void setBizInfo(Long bizId, String bizType) {
        // 此方法用于后续扩展，支持通过业务ID查询相关日志
    }

    /**
     * 构建动态查询条件
     */
    private Specification<OperationLog> buildSpecification(LogQueryRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getOperatorId() != null) {
                predicates.add(cb.equal(root.get("operatorId"), request.getOperatorId()));
            }

            if (request.getOperatorRole() != null) {
                predicates.add(cb.equal(root.get("operatorRole"), request.getOperatorRole()));
            }

            if (request.getModule() != null && !request.getModule().isEmpty()) {
                predicates.add(cb.equal(root.get("module"), request.getModule()));
            }

            if (request.getAction() != null && !request.getAction().isEmpty()) {
                predicates.add(cb.equal(root.get("action"), request.getAction()));
            }

            if (request.getStatus() != null && !request.getStatus().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }

            if (request.getOperatorName() != null && !request.getOperatorName().isEmpty()) {
                predicates.add(cb.like(root.get("operatorName"), "%" + request.getOperatorName() + "%"));
            }

            if (request.getStartTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createTime"), request.getStartTime()));
            }

            if (request.getEndTime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createTime"), request.getEndTime()));
            }

            if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
                Predicate descLike = cb.like(root.get("description"), "%" + request.getKeyword() + "%");
                Predicate urlLike = cb.like(root.get("requestUrl"), "%" + request.getKeyword() + "%");
                predicates.add(cb.or(descLike, urlLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 构建请求参数摘要
     */
    private String buildRequestParams(HttpServletRequest request) {
        try {
            Map<String, String[]> params = request.getParameterMap();
            if (params.isEmpty()) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            params.forEach((key, values) -> {
                // 过滤敏感字段
                if ("password".equalsIgnoreCase(key) || "token".equalsIgnoreCase(key)) {
                    sb.append(key).append("=***, ");
                } else if (values.length == 1) {
                    sb.append(key).append("=").append(values[0]).append(", ");
                } else {
                    sb.append(key).append("=").append(String.join(",", values)).append(", ");
                }
            });
            String result = sb.toString();
            return result.length() > 0 ? result.substring(0, result.length() - 2) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 日志统计类
     */
    @lombok.Data
    public static class LogStats {
        private long todayCount;
        private long weekCount;
        private long totalCount;
        private Map<String, Long> moduleStats;
        private Map<String, Long> statusStats;
    }
}
