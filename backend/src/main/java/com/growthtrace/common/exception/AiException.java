package com.growthtrace.common.exception;

import com.growthtrace.common.result.ResultCode;

public class AiException extends BusinessException {

    public AiException(ResultCode code, String message) {
        super(code, message);
    }

    public AiException(ResultCode code, String message, Throwable cause) {
        super(code, message, cause);
    }

    public static AiException unavailable(String detail, Throwable cause) {
        return new AiException(ResultCode.AI_UNAVAILABLE, detail, cause);
    }

    public static AiException parseError(String detail) {
        return new AiException(ResultCode.AI_PARSE_ERROR, detail);
    }

    public static AiException timeout(String detail) {
        return new AiException(ResultCode.AI_TIMEOUT, detail);
    }
}
