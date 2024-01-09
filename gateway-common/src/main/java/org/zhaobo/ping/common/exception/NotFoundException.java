package org.zhaobo.ping.common.exception;


import org.zhaobo.ping.common.enums.ResponseCode;

public class NotFoundException extends BaseException {

	private static final long serialVersionUID = -5534700534739261761L;

	public NotFoundException(ResponseCode code) {
		super(code.getMessage(), code);
	}
	
	public NotFoundException(Throwable cause, ResponseCode code) {
		super(code.getMessage(), cause, code);
	}
	
}
