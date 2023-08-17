package com.zjx.cointool.vo.jiedata;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class NetOutflowVO extends InOutFlowVO {
    private BigDecimal netOutUsd;


    public String generateMsg() {
        String msg;
        msg = "币种：" + this.getSymbol();
        msg = msg + "   净流出金额：：$" + getNetOutUsd() + "\n";
        return msg;
    }
}
