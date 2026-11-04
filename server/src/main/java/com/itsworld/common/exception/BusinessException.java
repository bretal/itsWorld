package com.itsworld.common.exception;

import com.itsworld.common.result.ErrorCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务异常
 * 所有业务相关的异常都应该抛出此异常
 *
 * @author Claude
 * @date 2026-10-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private Integer code;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 构造函数：使用ErrorCode枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    /**
     * 构造函数：自定义code和message
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造函数：仅message，code默认为1500（业务异常）
     */
    public BusinessException(String message) {
        super(message);
        this.code = ErrorCode.BUSINESS_ERROR.getCode();
        this.message = message;
    }

}
