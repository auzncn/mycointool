package com.zjx.cointool.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("watch_list")
public class WatchList {
    @TableId(type = IdType.INPUT)
    private String id;
    private String tag;
    private Integer active;
}
