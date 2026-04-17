package com.aisale.backend.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 *
 * 编码规则：8 位数字
 * - 前 2 位：模块码
 * - 中间 2 位：子模块码
 * - 后 4 位：具体错误码
 *
 * 模块划分：
 * - 10: 通用模块
 * - 20: 用户模块
 * - 30: 商品模块
 * - 40: 订单模块
 * - 50: 地址模块
 * - 60: 评价模块
 * - 70: 签到模块
 * - 80: 积分模块
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ==================== 通用模块 (10xxxxxx) ====================

    // 参数校验错误 (1001xxxx)
    PARAM_VALIDATION_ERROR("10010001", "参数校验失败", 400),
    REQUIRED_PARAM_MISSING("10010002", "缺少必填参数", 400),
    INVALID_PARAM_FORMAT("10010003", "参数格式错误", 400),

    // 资源相关错误 (1002xxxx)
    RESOURCE_NOT_FOUND("10020001", "资源不存在", 404),
    RESOURCE_ALREADY_EXISTS("10020002", "资源已存在", 409),
    RESOURCE_CONFLICT("10020003", "资源状态冲突", 409),

    // 认证授权错误 (1003xxxx)
    UNAUTHORIZED("10030001", "未授权访问", 401),
    TOKEN_INVALID("10030002", "Token 无效或已过期", 401),
    FORBIDDEN("10030003", "禁止访问", 403),
    PERMISSION_DENIED("10030004", "权限不足", 403),

    // 系统错误 (1099xxxx)
    INTERNAL_SERVER_ERROR("10990001", "系统内部错误", 500),
    DATABASE_ERROR("10990002", "数据库操作失败", 500),
    EXTERNAL_SERVICE_ERROR("10990003", "外部服务调用失败", 503),

    // 文件上传错误 (1004xxxx)
    FILE_UPLOAD_ERROR("10040001", "文件上传失败", 500),
    FILE_DOWNLOAD_ERROR("10040002", "文件下载失败", 500),
    FILE_DELETE_ERROR("10040003", "文件删除失败", 500),
    FILE_EMPTY("10040004", "上传文件不能为空", 400),
    FILE_SIZE_EXCEEDED("10040005", "文件大小超过限制", 400),
    FILE_TYPE_NOT_ALLOWED("10040006", "不支持的文件类型", 400),

    // ==================== 用户模块 (20xxxxxx) ====================

    // 用户认证错误 (2001xxxx)
    USER_NOT_FOUND("20010001", "用户不存在", 404),
    USER_ALREADY_EXISTS("20010002", "用户名已被注册", 409),
    EMAIL_ALREADY_EXISTS("20010003", "邮箱已被注册", 409),
    PHONE_ALREADY_EXISTS("20010004", "手机号已被注册", 409),

    // 用户状态错误 (2002xxxx)
    USER_BANNED("20020001", "账号已被禁用", 403),
    USER_INACTIVE("20020002", "账号未激活", 403),
    PASSWORD_ERROR("20020003", "密码错误", 401),
    VERIFICATION_CODE_ERROR("20020004", "验证码错误", 400),
    VERIFICATION_CODE_EXPIRED("20020005", "验证码已过期", 400),

    // ==================== 商品模块 (30xxxxxx) ====================

    // 商品相关错误 (3001xxxx)
    PRODUCT_NOT_FOUND("30010001", "商品不存在", 404),
    PRODUCT_ALREADY_EXISTS("30010002", "商品已存在", 409),
    PRODUCT_OFF_SALE("30010003", "商品已下架", 400),
    PRODUCT_OUT_OF_STOCK("30010004", "商品库存不足", 400),

    // 商品操作错误 (3002xxxx)
    PRODUCT_OPERATION_DENIED("30020001", "无权操作此商品", 403),
    PRODUCT_VIEW_DENIED("30020002", "无权查看此商品", 403),

    // ==================== 订单模块 (40xxxxxx) ====================

    // 订单相关错误 (4001xxxx)
    ORDER_NOT_FOUND("40010001", "订单不存在", 404),
    ORDER_ALREADY_EXISTS("40010002", "订单已存在", 409),

    // 订单状态错误 (4002xxxx)
    ORDER_STATUS_CONFLICT("40020001", "订单状态冲突", 409),
    ORDER_PAYMENT_NOT_ALLOWED("40020002", "订单状态不允许付款", 400),
    ORDER_PICKUP_NOT_ALLOWED("40020003", "订单状态不允许提货", 400),
    ORDER_CONFIRM_NOT_ALLOWED("40020004", "订单状态不允许确认", 400),
    ORDER_CANCEL_NOT_ALLOWED("40020005", "订单状态不允许取消", 400),

    // 订单操作错误 (4003xxxx)
    ORDER_OPERATION_DENIED("40030001", "无权操作此订单", 403),
    ORDER_VIEW_DENIED("40030002", "无权查看此订单", 403),
    ORDER_ALREADY_PAID("40030003", "该商品已有进行中的订单", 409),
    SELF_PURCHASE_DENIED("40030004", "不能购买自己的商品", 400),

    // ==================== 地址模块 (50xxxxxx) ====================

    // 地址相关错误 (5001xxxx)
    ADDRESS_NOT_FOUND("50010001", "地址不存在", 404),
    ADDRESS_LIMIT_EXCEEDED("50010002", "地址数量已达上限", 400),

    // 地址操作错误 (5002xxxx)
    ADDRESS_OPERATION_DENIED("50020001", "无权操作此地址", 403),

    // ==================== 评价模块 (60xxxxxx) ====================

    // 评价相关错误 (6001xxxx)
    REVIEW_NOT_FOUND("60010001", "评价不存在", 404),
    REVIEW_ALREADY_EXISTS("60010002", "已经评价过了", 409),
    REVIEW_NOT_ALLOWED("60010003", "当前订单状态不允许评价", 400),

    // 评价操作错误 (6002xxxx)
    REVIEW_OPERATION_DENIED("60020001", "无权操作此评价", 403),

    // ==================== 签到模块 (70xxxxxx) ====================

    // 签到相关错误 (7001xxxx)
    CHECK_IN_ALREADY_DONE("70010001", "今日已签到", 409),
    CHECK_IN_NOT_FOUND("70010002", "签到记录不存在", 404),

    // ==================== 积分模块 (80xxxxxx) ====================

    // 积分相关错误 (8001xxxx)
    POINT_INSUFFICIENT("80010001", "积分不足", 400),
    POINT_ACCOUNT_NOT_FOUND("80010002", "积分账户不存在", 404),

    // ==================== 信誉模块 (90xxxxxx) ====================

    // 信誉相关错误 (9001xxxx)
    REPUTATION_ACCOUNT_NOT_FOUND("90010001", "信誉账户不存在", 404);

    /**
     * 错误码
     */
    private final String code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * HTTP 状态码
     */
    private final int httpStatus;

}
