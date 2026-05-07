package com.tradeagent.common;

public enum ErrorCode {

    // Common
    INVALID_INPUT("C001", "잘못된 입력값입니다."),
    RESOURCE_NOT_FOUND("C002", "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR("C003", "서버 내부 오류가 발생했습니다."),

    // External API
    EXTERNAL_API_ERROR("E001", "외부 API 호출 중 오류가 발생했습니다."),
    ALPACA_API_ERROR("E002", "Alpaca API 호출 중 오류가 발생했습니다."),
    GDELT_API_ERROR("E003", "GDELT API 호출 중 오류가 발생했습니다."),
    VLLM_API_ERROR("E004", "vLLM API 호출 중 오류가 발생했습니다."),

    // Portfolio
    PORTFOLIO_NOT_FOUND("P001", "포트폴리오를 찾을 수 없습니다."),
    POSITION_NOT_FOUND("P002", "보유 포지션을 찾을 수 없습니다."),
    TRADE_NOT_FOUND("P003", "거래 내역을 찾을 수 없습니다."),
    INSUFFICIENT_QUANTITY("P004", "매도 수량이 보유 수량을 초과합니다."),

    // Evaluation
    EVALUATION_NOT_FOUND("V001", "평가 데이터를 찾을 수 없습니다."),

    // Sector
    SECTOR_NOT_FOUND("S001", "섹터 정보를 찾을 수 없습니다.");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
