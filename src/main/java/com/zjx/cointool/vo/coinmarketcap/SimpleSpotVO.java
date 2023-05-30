package com.zjx.cointool.vo.coinmarketcap;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SimpleSpotVO {
    private String baseSymbol;
    private BigDecimal price;
    private BigDecimal bnVolumeUsd;
    private BigDecimal upVolumeUsd;

    public String generateMsg() {
        String msg;
        msg = "币种：" + baseSymbol + " 价格：" + price.toString() + "\n";
        msg = msg + "币安交易所成交量：$" + bnVolumeUsd.toString() + "\n";
        msg = msg + "upBit交易所成交量：$" + upVolumeUsd.toString() + "\n";
        msg = msg + "______________________________\n";
        return msg;
    }
}
