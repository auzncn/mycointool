/**
 * Copyright 2023 json.cn
 */
package com.zjx.cointool.vo.debank;

import lombok.Data;

import java.util.List;

/**
 * Auto-generated: 2023-04-20 16:19:53
 *
 * @author json.cn (i@json.cn)
 * @website http://www.json.cn/java2pojo/
 */
@Data
public class Tx {
    private String from_addr;
    private String name;
    private List<String> params;
    private int status;
    private String to_addr;
    private Double value;
}