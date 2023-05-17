package com.zjx.cointool.vo.debank;

import lombok.Data;

@Data
public class Sends {
    private String amount;
    private String price;
    private String to_addr;
    private String token_id;
}
