package com.itsworld.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一API返回值类
 * 所有API响应都使用此格式包装
 *
 * @param <T> 业务数据类型
 * @author Claude
 * @date 2026-10-30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    /**
     * 返回码：0表示成功，其他值表示失败
     */
    private Integer code;

    /**
     * 返回消息
     */
    private String message;

    /**
     * 业务数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 构造函数：成功响应（仅数据）
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(0)
                .message("success")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 构造函数：成功响应（数据+消息）
     */
    public static <T> Result<T> success(T data, String message) {
        return Result.<T>builder()
                .code(0)
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 构造函数：失败响应（code+message）
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 构造函数：失败响应（ErrorCode枚举）
     */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return Result.<T>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .data(null)
                .timestamp(System.currentTimeMillis())
                .build();
    }

}
