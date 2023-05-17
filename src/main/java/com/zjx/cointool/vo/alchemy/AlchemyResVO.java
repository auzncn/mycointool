package com.zjx.cointool.vo.alchemy;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class AlchemyResVO {
    private String blockNum;
    private String uniqueId;
    private String hash;
    private String from;
    private String to;
    private BigDecimal value;
    private String erc721TokenId;
    private String erc1155Metadata;
    private String tokenId;
    private String asset;
    private String category;
    private Metadata metadata;
    private RawContract rawContract;

    @Data
    public static class Metadata {
        private Date blockTimestamp;
    }

    @Data
    public static class RawContract {
        private String address;
    }
}
