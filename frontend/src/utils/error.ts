/**
 * 错误工具类
 * 提供错误码映射和错误消息格式化
 */

/**
 * 错误码到用户友好消息的映射
 * 按模块分类，便于维护和扩展
 */
export const ERROR_MESSAGES: Record<string, string> = {
  // ==================== 通用错误 (10xxxxxx) ====================
  'PARAM_VALIDATION_ERROR': '表单填写有误，请检查后重试',
  'REQUIRED_PARAM_MISSING': '请填写完整信息',
  'INVALID_PARAM_FORMAT': '参数格式错误',
  'RESOURCE_NOT_FOUND': '请求的内容不存在',
  'RESOURCE_CONFLICT': '操作冲突，请刷新后重试',
  'UNAUTHORIZED': '请先登录',
  'TOKEN_INVALID': '登录已过期，请重新登录',
  'FORBIDDEN': '无权访问',
  'PERMISSION_DENIED': '权限不足',
  'INTERNAL_SERVER_ERROR': '系统繁忙，请稍后重试',
  'DATABASE_ERROR': '数据库操作失败，请稍后重试',
  'EXTERNAL_SERVICE_ERROR': '外部服务异常，请稍后重试',

  // ==================== 用户模块错误 (20xxxxxx) ====================
  'USER_NOT_FOUND': '用户不存在',
  'USER_ALREADY_EXISTS': '用户名已被使用，请更换',
  'EMAIL_ALREADY_EXISTS': '该邮箱已被注册',
  'PHONE_ALREADY_EXISTS': '该手机号已被注册',
  'USER_BANNED': '账号已被禁用，请联系客服',
  'USER_INACTIVE': '账号未激活',
  'PASSWORD_ERROR': '密码错误，请重试',
  'VERIFICATION_CODE_ERROR': '验证码错误',
  'VERIFICATION_CODE_EXPIRED': '验证码已过期，请重新获取',

  // ==================== 商品模块错误 (30xxxxxx) ====================
  'PRODUCT_NOT_FOUND': '商品已下架或不存在',
  'PRODUCT_ALREADY_EXISTS': '商品已存在',
  'PRODUCT_OFF_SALE': '商品已下架',
  'PRODUCT_OUT_OF_STOCK': '商品库存不足',
  'PRODUCT_OPERATION_DENIED': '无权操作此商品',
  'PRODUCT_VIEW_DENIED': '无权查看此商品',

  // ==================== 订单模块错误 (40xxxxxx) ====================
  'ORDER_NOT_FOUND': '订单不存在',
  'ORDER_ALREADY_EXISTS': '订单已存在',
  'ORDER_STATUS_CONFLICT': '订单状态冲突，请刷新后重试',
  'ORDER_PAYMENT_NOT_ALLOWED': '当前订单状态无法付款',
  'ORDER_PICKUP_NOT_ALLOWED': '当前订单状态无法提货',
  'ORDER_CONFIRM_NOT_ALLOWED': '当前订单状态无法确认',
  'ORDER_CANCEL_NOT_ALLOWED': '当前订单状态无法取消',
  'ORDER_OPERATION_DENIED': '无权操作此订单',
  'ORDER_VIEW_DENIED': '无权查看此订单',
  'ORDER_ALREADY_PAID': '该商品已有未完成订单',
  'SELF_PURCHASE_DENIED': '无法购买自己的商品',

  // ==================== 地址模块错误 (50xxxxxx) ====================
  'ADDRESS_NOT_FOUND': '地址不存在',
  'ADDRESS_LIMIT_EXCEEDED': '最多只能添加 10 个地址',
  'ADDRESS_OPERATION_DENIED': '无权操作此地址',

  // ==================== 评价模块错误 (60xxxxxx) ====================
  'REVIEW_NOT_FOUND': '评价不存在',
  'REVIEW_ALREADY_EXISTS': '您已经评价过了',
  'REVIEW_NOT_ALLOWED': '当前订单状态不允许评价',
  'REVIEW_OPERATION_DENIED': '无权操作此评价'
}

/**
 * HTTP 状态码到错误类型的映射
 */
export const HTTP_STATUS_MAP: Record<number, { type: string; message: string }> = {
  400: { type: 'warning', message: '请求参数错误' },
  401: { type: 'error', message: '登录已过期，请重新登录' },
  403: { type: 'error', message: '拒绝访问' },
  404: { type: 'warning', message: '请求的资源不存在' },
  409: { type: 'warning', message: '操作冲突' },
  500: { type: 'error', message: '服务器内部错误' },
  503: { type: 'error', message: '服务暂时不可用' }
}

/**
 * 获取用户友好消息
 * @param code 错误码
 * @param defaultMessage 默认消息
 * @returns 用户友好消息
 */
export function getUserFriendlyMessage(code: string | number, defaultMessage: string): string {
  const codeStr = String(code)
  return ERROR_MESSAGES[codeStr] || defaultMessage || '操作失败，请稍后重试'
}

/**
 * 判断是否需要跳转登录
 * @param code 错误码
 * @returns 是否需要跳转登录
 */
export function shouldRedirectToLogin(code: string | number): boolean {
  const codeStr = String(code)
  return ['UNAUTHORIZED', 'TOKEN_INVALID'].includes(codeStr)
}

/**
 * 判断是否是业务错误（可恢复的错误）
 * @param code 错误码
 * @returns 是否是业务错误
 */
export function isBusinessError(code: string | number): boolean {
  const codeStr = String(code)
  // 4xx 状态码的错误通常是业务错误
  return codeStr.startsWith('1') || codeStr.startsWith('2') ||
         codeStr.startsWith('3') || codeStr.startsWith('4') ||
         codeStr.startsWith('5') || codeStr.startsWith('6')
}

/**
 * 获取错误类型（用于 UI 展示）
 * @param code 错误码
 * @returns 错误类型：error | warning | info
 */
export function getErrorType(code: string | number): 'error' | 'warning' | 'info' {
  const codeStr = String(code)
  if (codeStr.startsWith('1099')) {
    // 系统错误
    return 'error'
  }
  if (codeStr.startsWith('1003')) {
    // 认证授权错误
    return 'error'
  }
  if (codeStr.startsWith('2002') || codeStr.startsWith('4002')) {
    // 状态类错误
    return 'warning'
  }
  // 其他业务错误
  return 'warning'
}
