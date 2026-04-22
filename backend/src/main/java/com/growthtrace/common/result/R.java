package com.growthtrace.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> implements Serializable {

    private int code;
    private String message;
    private T data;
    private long timestamp = System.currentTimeMillis();

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = ResultCode.OK.getCode();
        r.message = ResultCode.OK.getMessage();
        r.data = data;
        return r;
    }

    public static <T> R<T> fail(ResultCode code) {
        return fail(code, code.getMessage());
    }

    public static <T> R<T> fail(ResultCode code, String message) {
        R<T> r = new R<>();
        r.code = code.getCode();
        r.message = message;
        return r;
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
