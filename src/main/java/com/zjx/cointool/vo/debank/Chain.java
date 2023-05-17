package com.zjx.cointool.vo.debank;

import lombok.Data;

import java.util.Date;

@Data
public class Chain {
    private double block_interval;
    private String explorer_host;
    private String id;
    private boolean is_support_archive;
    private boolean is_support_history;
    private String logo_url;
    private String name;
    private int network_id;
    private String prefix;
    private Date start_at;
    private String svg_logo_url;
    private String token_id;
    private String token_symbol;
    private String wrapped;
}
