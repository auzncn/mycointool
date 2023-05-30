package com.zjx.cointool.vo.coinmarketcap;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ExchangesSpotVO {
    private int exchangeId;
    private String exchangeName;
    private String exchangeSlug;
    private int outlierDetected;
    private int priceExcluded;
    private int volumeExcluded;
    private int marketId;
    private String marketPair;
    private String category;
    private String marketUrl;
    private String marketScore;
    private int marketReputation;
    private String baseSymbol;
    private int baseCurrencyId;
    private String baseCurrencyName;
    private String baseCurrencySlug;
    private String quoteSymbol;
    private int quoteCurrencyId;
    private BigDecimal price;
    private BigDecimal volumeUsd;
    private int effectiveLiquidity;
    private Date lastUpdated;
    private BigDecimal quote;
    private BigDecimal volumeBase;
    private BigDecimal volumeQuote;
    private String feeType;
    private BigDecimal depthUsdNegativeTwo;
    private BigDecimal depthUsdPositiveTwo;
}
