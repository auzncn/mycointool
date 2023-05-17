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
public class History_list {
    private String cate_id;
    private String cex_id;
    private String chain;
    private String id;
    private boolean is_scam;
    private String other_addr;
    private String project_id;
    private List<Receives> receives;
    private List<Sends> sends;
    private long time_at;
    private String token_approve;
    private Tx tx;
}