package com.zjx.cointool.vo.alchemy;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class TransactionVO {
    private String userId;
    private String tag;
    private String hash;
    private Date time;
    private List<SendReceive> receive;
    private List<SendReceive> send;

    @Data
    public static class SendReceive {
        private BigDecimal value;
        private String asset;
        private String category;
        private String sendTo;
        private String receiveFrom;
    }

    public List<String> generateMsg() {
        List<String> list = new ArrayList<>();
        list.add(DateUtil.formatDateTime(time));
        list.add(tag + "(" + userId + ")");
        if (CollectionUtil.isNotEmpty(send)) {
            send.forEach(e -> {
                list.add("发送：" + e.getAsset() + "，数量：" + e.getValue() + "，到地址" + e.getSendTo());
            });
        }
        if (CollectionUtil.isNotEmpty(receive)) {
            receive.forEach(e -> {
                list.add("从地址" + e.receiveFrom + "，接收：" + e.getAsset() + "，数量：" + e.getValue());
            });
        }
        return list;
    }
}
