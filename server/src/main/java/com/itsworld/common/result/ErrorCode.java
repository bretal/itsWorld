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
