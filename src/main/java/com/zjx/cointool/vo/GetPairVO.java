package com.zjx.cointool.vo;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class GetPairVO {
    @ExcelProperty("币种")
    private String baseSymbol;
    @ExcelIgnore
    private String quoteSymbol;
    @ExcelProperty("交易对")
    private String marketPair;
    @ExcelProperty("价格")
    private String price;
    @ExcelProperty("成交额")
    private String volumeUsd;

}
