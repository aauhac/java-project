package com.tradeagent.common;

public enum TradeType {

    BUY("매수"),
    SELL("매도");

    private final String label;

    TradeType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
