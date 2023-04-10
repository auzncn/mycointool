package com.zjx.cointool.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.Date;

@Data
public class ATHVO {
    @ExcelProperty(value = "价格",index = 3)
    private double current_price_usd;
    @ExcelProperty(value = "币种",index = 1)
    private String code;
    @ExcelProperty(value = "全称",index = 2)
    private String name;
    @ExcelProperty(value = "排名",index = 0)
    private int rank;
    @ExcelProperty(value = "最高价",index = 4)
    private double high_price;
    @ExcelProperty(value = "ATH跌幅",index = 8)
    private double drop_ath;
    @ExcelProperty(value = "最低价",index = 6)
    private double low_price;
    @ExcelProperty(value = "最高价时间",index = 5)
    private Date high_time;
    @ExcelProperty(value = "最低价时间",index = 7)
    private Date low_time;
}
