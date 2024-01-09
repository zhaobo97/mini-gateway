package org.zhaobo.ping.common.exception;

import org.zhaobo.ping.common.enums.ResponseCode;

public class RateLimitException extends BaseException{

    public RateLimitException(String message, ResponseCode code) {
        super(message, code);
    }

    public RateLimitException() {
        super();
    }
}
