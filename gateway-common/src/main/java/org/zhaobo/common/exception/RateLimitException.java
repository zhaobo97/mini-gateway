package org.zhaobo.common.exception;

import org.zhaobo.common.enums.ResponseCode;

public class RateLimitException extends BaseException{

    public RateLimitException(String message, ResponseCode code) {
        super(message, code);
    }

    public RateLimitException() {
        super();
    }
}
