package com.tradeagent.common;

public class ExternalApiException extends BusinessException {

    public ExternalApiException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ExternalApiException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
