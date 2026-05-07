package com.tradeagent.market;

import com.tradeagent.common.ErrorCode;
import com.tradeagent.common.ExternalApiException;
import com.tradeagent.common.ValidationException;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public abstract class AbstractMarketDataClient implements MarketDataClient {

    protected String normalizeSymbol(String symbol) {
        validateSymbol(symbol);
        return symbol.trim().toUpperCase();
    }

    protected void validateSymbol(String symbol) {
        if (!StringUtils.hasText(symbol)) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "symbol must not be blank");
        }
    }

    protected ExternalApiException handleApiError(Exception e) {
        if (e instanceof ExternalApiException externalApiException) {
            return externalApiException;
        }
        if (e instanceof WebClientResponseException responseException) {
            String detail = responseException.getResponseBodyAsString();
            if (!StringUtils.hasText(detail)) {
                detail = responseException.getStatusCode().toString();
            }
            return new ExternalApiException(ErrorCode.ALPACA_API_ERROR, "Alpaca API request failed: " + detail);
        }
        if (e instanceof WebClientRequestException requestException) {
            return new ExternalApiException(ErrorCode.ALPACA_API_ERROR,
                    "Failed to reach Alpaca API: " + requestException.getMessage());
        }
        return new ExternalApiException(ErrorCode.ALPACA_API_ERROR, "Unexpected Alpaca API error: " + e.getMessage());
    }
}
