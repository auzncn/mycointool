package com.zjx.cointool.task;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson2.JSONObject;
import com.zjx.cointool.util.DingUtil;
import com.zjx.cointool.vo.coinmarketcap.ExchangesSpotVO;
import com.zjx.cointool.vo.coinmarketcap.SimpleSpotVO;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class KimchiTask {
    private static final String BINANCE = "https://api.coinmarketcap.com/data-api/v3/exchange/market-pairs/latest?slug=binance&category=spot&start=1&limit=100";
    private static final String UPBIT = "https://api.coinmarketcap.com/data-api/v3/exchange/market-pairs/latest?slug=upbit&category=spot&start=1&limit=100";
    @Resource
    private DingUtil dingUtil;

    @Scheduled(cron = "0 0 8,20 * * ?")
    public void kimchiPremium() {
        String binanceSpot = HttpRequest.get(BINANCE)
                .setHttpProxy("127.0.0.1", 1080)
                .execute().body();
        JSONObject binanceJson = JSONObject.parseObject(binanceSpot);
        String binanceStatus = binanceJson.getJSONObject("status").getString("error_message");

        String upbitSpot = HttpRequest.get(UPBIT)
                .setHttpProxy("127.0.0.1", 1080)
                .execute().body();
        JSONObject upbitJson = JSONObject.parseObject(upbitSpot);
        String upbitStatus = upbitJson.getJSONObject("status").getString("error_message");
        List<SimpleSpotVO> dataList = new ArrayList<>();
        if ("SUCCESS".equalsIgnoreCase(binanceStatus) && "SUCCESS".equalsIgnoreCase(upbitStatus)) {
            List<ExchangesSpotVO> bList = binanceJson.getJSONObject("data").getJSONArray("marketPairs").toJavaList(ExchangesSpotVO.class);
            List<ExchangesSpotVO> uList = upbitJson.getJSONObject("data").getJSONArray("marketPairs").toJavaList(ExchangesSpotVO.class);

            Map<Integer, List<ExchangesSpotVO>> bMap = bList.stream().collect(Collectors.groupingBy(ExchangesSpotVO::getBaseCurrencyId));
            Map<String, SimpleSpotVO> bCollect = bMap.keySet().stream().map(e -> {
                List<ExchangesSpotVO> exchangesSpotVOS = bMap.get(e);
                Optional<BigDecimal> reduce = exchangesSpotVOS.stream().map(ExchangesSpotVO::getVolumeUsd).reduce(BigDecimal::add);
                ExchangesSpotVO exchangesSpotVO = exchangesSpotVOS.get(0);
                SimpleSpotVO simpleSpotVO = new SimpleSpotVO();
                simpleSpotVO.setBaseSymbol(exchangesSpotVO.getBaseSymbol());
                simpleSpotVO.setPrice(exchangesSpotVO.getPrice());
                simpleSpotVO.setBnVolumeUsd(reduce.get());
                return simpleSpotVO;
            }).collect(Collectors.toMap(SimpleSpotVO::getBaseSymbol, p -> p));

            Map<Integer, List<ExchangesSpotVO>> uMap = uList.stream().collect(Collectors.groupingBy(ExchangesSpotVO::getBaseCurrencyId));
            Map<String, SimpleSpotVO> uCollect = uMap.keySet().stream().map(e -> {
                List<ExchangesSpotVO> exchangesSpotVOS = uMap.get(e);
                Optional<BigDecimal> reduce = exchangesSpotVOS.stream().map(ExchangesSpotVO::getVolumeUsd).reduce(BigDecimal::add);
                ExchangesSpotVO exchangesSpotVO = exchangesSpotVOS.get(0);
                SimpleSpotVO simpleSpotVO = new SimpleSpotVO();
                simpleSpotVO.setBaseSymbol(exchangesSpotVO.getBaseSymbol());
                simpleSpotVO.setPrice(exchangesSpotVO.getPrice());
                simpleSpotVO.setUpVolumeUsd(reduce.get());
                return simpleSpotVO;
            }).collect(Collectors.toMap(SimpleSpotVO::getBaseSymbol, p -> p));


            uCollect.keySet().forEach(e -> {
                SimpleSpotVO uSpot = uCollect.get(e);
                SimpleSpotVO bSpot = bCollect.get(e);
                if (bSpot != null && uSpot.getUpVolumeUsd().compareTo(bSpot.getBnVolumeUsd()) > 0) {
                    uSpot.setBnVolumeUsd(bSpot.getBnVolumeUsd());
                    dataList.add(uSpot);
                }
            });

            if (CollUtil.isNotEmpty(dataList)) {
                StringBuilder msg = new StringBuilder(DateUtil.now() + " 泡菜溢价查询\n");
                for (SimpleSpotVO simpleSpotVO : dataList) {
                    msg.append(simpleSpotVO.generateMsg());
                }
                dingUtil.ding(msg.toString());
            }

        }
    }
}
