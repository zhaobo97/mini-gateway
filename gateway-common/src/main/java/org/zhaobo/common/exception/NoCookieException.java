package org.zhaobo.common.exception;

import org.zhaobo.common.enums.ResponseCode;

public class NoCookieException extends BaseException{

    public NoCookieException() {
        super();
    }

    public NoCookieException(String message, ResponseCode code) {
        super(message, code);
    }
}
