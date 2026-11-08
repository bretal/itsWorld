package com.itsworld.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 * 定义系统中所有可能返回的错误码
 *
 * @author Claude
 * @date 2026-10-30
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 成功
    SUCCESS(0, "成功"),

    // 参数相关 (400-499)
    INVALID_PARAM(400, "参数错误"),
    INVALID_REQUEST(400, "请求错误"),

    // 认证授权 (401-403)
    UNAUTHORIZED(401, "未授权，请登录"),
    FORBIDDEN(403, "无权限访问"),

    // 资源相关 (404-410)
    NOT_FOUND(404, "资源不存在"),
    RESOURCE_CONFLICT(409, "资源冲突"),

    // 服务异常 (500-599)
    SERVER_ERROR(500, "服务异常"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    // 用户相关 (1000-1099)
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    PHONE_ALREADY_EXISTS(1006, "手机号已注册"),
    PASSWORD_ERROR(1003, "密码错误"),
    PASSWORD_TOO_SHORT(1004, "密码长度不能少于8个字符"),
    USER_DISABLED(1005, "用户已禁用"),

    // 宠物相关 (1100-1199)
    PET_NOT_FOUND(1101, "宠物不存在"),
    PET_ALREADY_EXISTS(1102, "宠物已存在"),

    // 日记相关 (1200-1299)
    DIARY_NOT_FOUND(1201, "日记不存在"),
    DIARY_ALREADY_EXISTS(1202, "该天已有日记"),

    // 文件相关 (1300-1399)
    FILE_UPLOAD_ERROR(1301, "文件上传失败"),
    FILE_TOO_LARGE(1302, "文件过大"),
    FILE_TYPE_NOT_ALLOWED(1303, "不支持的文件类型"),

    // JWT相关 (1400-1499)
    TOKEN_INVALID(1401, "Token无效"),
    TOKEN_EXPIRED(1402, "Token已过期"),

    // 健康管理相关 (1600-1699)
    HEALTH_RECORD_NOT_FOUND(1601, "健康记录不存在"),

    // 会员相关 (1700-1799)
    MEMBER_ACCOUNT_NOT_FOUND(1701, "会员账户不存在"),
    MEMBER_LEVEL_NOT_FOUND(1702, "会员等级不存在"),
    MEMBER_LEVEL_CODE_EXISTS(1703, "等级编码已存在"),
    MEMBER_LEVEL_THRESHOLD_EXISTS(1704, "成长值门槛已存在"),
    MEMBER_LEVEL_LAST_NORMAL(1705, "至少保留一个成长值为0的启用等级"),
    MEMBER_TASK_NOT_FOUND(1706, "会员任务不存在"),
    MEMBER_TASK_CODE_EXISTS(1707, "任务编码已存在"),
    MEMBER_TASK_NOT_COMPLETED(1708, "任务尚未完成"),
    MEMBER_TASK_ALREADY_CLAIMED(1709, "任务奖励已领取"),
    COUPON_TEMPLATE_NOT_FOUND(1710, "优惠券模板不存在"),
    COUPON_STOCK_EMPTY(1711, "优惠券库存不足"),
    COUPON_ISSUE_INVALID(1712, "发券数量不合法"),
    GIFT_NOT_FOUND(1713, "礼包不存在"),
    GIFT_NOT_AVAILABLE(1714, "礼包不可领取"),
    GIFT_ALREADY_CLAIMED(1715, "礼包已领取"),
    GIFT_LEVEL_NOT_ENOUGH(1716, "会员等级不足"),
    MEMBER_AWARD_DUPLICATE(1717, "奖励已发放，请勿重复操作"),
    COUPON_NOT_AVAILABLE(1718, "优惠券不可用"),
    COUPON_LOCK_FAILED(1719, "优惠券锁定失败"),

    // 钱包相关 (1800-1899)
    WALLET_ACCOUNT_NOT_FOUND(1801, "钱包账户不存在"),
    RECHARGE_PLAN_NOT_FOUND(1802, "充值方案不存在"),
    RECHARGE_ORDER_NOT_FOUND(1803, "充值单不存在"),
    RECHARGE_STATUS_ERROR(1804, "充值单状态不允许此操作"),
    RECHARGE_ALREADY_CREDITED(1805, "充值单已入账"),
    MOCK_PAY_DISABLED(1806, "模拟支付未开启"),
    RECHARGE_PLAN_INVALID(1807, "充值方案参数不合法"),
    WALLET_BALANCE_INSUFFICIENT(1808, "钱包可用余额不足"),

    // 商业服务-预约与套餐 (1900-1999)
    COMMERCE_STORE_NOT_AVAILABLE(1901, "门店当前不可预约"),
    COMMERCE_SERVICE_NOT_AVAILABLE(1902, "服务在该门店不可用"),
    BOOKING_SLOT_NOT_FOUND(1903, "预约时段不存在"),
    BOOKING_SLOT_UNAVAILABLE(1904, "预约时段不可用"),
    BOOKING_SLOT_FULL(1905, "该时段已约满"),
    BOOKING_PET_NOT_ELIGIBLE(1906, "宠物不符合该服务的适配条件"),
    BOOKING_NOT_FOUND(1907, "预约不存在"),
    BOOKING_STATUS_ERROR(1908, "预约状态不允许此操作"),
    PACKAGE_NOT_FOUND(1909, "套餐不存在"),
    PACKAGE_NOT_AVAILABLE(1910, "套餐不可购买"),
    PACKAGE_SELECTION_INVALID(1911, "套餐选项不合法"),
    USER_PACKAGE_NOT_FOUND(1912, "用户套餐不存在"),
    USER_PACKAGE_NOT_AVAILABLE(1913, "用户套餐不可用"),
    PACKAGE_TIMES_INSUFFICIENT(1914, "套餐次数不足"),

    // 商业服务-物流/售后/评价 (1960-1999)
    ORDER_NOT_SHIPPABLE(1961, "订单当前状态不可发货"),
    ORDER_ALREADY_SHIPPED(1962, "订单已发货"),
    SHIPMENT_NOT_FOUND(1963, "发货记录不存在"),
    SHIPMENT_NOT_RECEIVABLE(1964, "发货记录当前状态不可确认收货"),
    AFTER_SALE_NOT_FOUND(1965, "售后单不存在"),
    AFTER_SALE_STATUS_ERROR(1966, "售后状态不允许此操作"),
    AFTER_SALE_NOT_ALLOWED(1967, "当前订单不可申请售后"),
    AFTER_SALE_AMOUNT_INVALID(1968, "售后金额不合法"),
    AFTER_SALE_DUPLICATE(1969, "该订单项已有处理中的售后"),
    REFUND_EXECUTE_FAILED(1970, "退款执行失败"),
    REVIEW_NOT_FOUND(1971, "评价不存在"),
    REVIEW_STATUS_ERROR(1972, "评价状态不允许此操作"),
    REVIEW_ALREADY_EXISTS(1973, "订单已评价"),
    REVIEW_NOT_ALLOWED(1974, "当前订单不可评价"),

    // 其他业务错误 (1500+)
    BUSINESS_ERROR(1500, "业务异常"),
    ;

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误信息
     */
    private final String message;

}
