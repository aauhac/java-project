package com.tradeagent.common;

public class GdeltRateLimitException extends ExternalApiException {

    public GdeltRateLimitException(String detail) {
        super(ErrorCode.GDELT_API_ERROR, detail);
    }
}
