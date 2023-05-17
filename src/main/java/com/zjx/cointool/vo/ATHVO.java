package com.zjx.cointool.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.Date;

@Data
public class ATHVO {
    @ExcelProperty(value = "市值排名", index = 0)
    private int market_cap_rank;
    @ExcelProperty(value = "币种", index = 1)
    private String symbol;
    @ExcelProperty(value = "全称", index = 2)
    private String name;
    @ExcelProperty(value = "市值", index = 3)
    private long market_cap;
    @ExcelProperty(value = "当前价格", index = 4)
    private double current_price;
    @ExcelProperty(value = "ATH", index = 5)
    private double ath;
    @ExcelProperty(value = "ATH时间", index = 6)
    private Date ath_date;
    @ExcelProperty(value = "ATL", index = 7)
    private double atl;
    @ExcelProperty(value = "ATL时间", index = 8)
    private Date atl_date;
    @ExcelProperty(value = "ATH跌幅", index = 9)
    private double ath_change_percentage;
    @ExcelProperty(value = "ATL涨幅", index = 10)
    private double atl_change_percentage;
    private double drop_ath;


}
