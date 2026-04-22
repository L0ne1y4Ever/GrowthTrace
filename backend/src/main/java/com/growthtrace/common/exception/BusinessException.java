package com.growthtrace.common.exception;

import com.growthtrace.common.result.ResultCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ResultCode code;

    public BusinessException(ResultCode code) {
        super(code.getMessage());
        this.code = code;
    }

    public BusinessException(ResultCode code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
