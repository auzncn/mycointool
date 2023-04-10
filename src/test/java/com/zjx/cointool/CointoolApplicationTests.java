package com.zjx.cointool;

import cn.hutool.core.date.DateUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zjx.cointool.vo.ATHVO;
import com.zjx.cointool.vo.GetPairVO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.stream.Collectors;

@SpringBootTest
class CointoolApplicationTests {

    @Test
    void getBUSDPair() {
        List<GetPairVO> pairList = new ArrayList<>();
        String url = "https://api.coinmarketcap.com/data-api/v3/exchange/market-pairs/latest?slug=binance&category=spot&start=%d&limit=1000";
        int start = 1;
        while (true) {
            String format = String.format(url, start);
            String result = HttpRequest.get(format)
                    .setHttpProxy("127.0.0.1", 1080)
                    .execute().body();
            JSONObject jsonObject = JSONObject.parseObject(result);
            Integer errorCode = jsonObject.getJSONObject("status").getInteger("error_code");
            if (errorCode == 0) {
                JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("marketPairs");
                if (jsonArray.isEmpty()) {
                    break;
                }
                List<GetPairVO> list = jsonArray.toJavaList(GetPairVO.class);
                pairList.addAll(list);
                start += 1000;
            } else {
                break;
            }
        }
        Map<String, List<GetPairVO>> collect = pairList.stream().collect(Collectors.groupingBy(GetPairVO::getBaseSymbol));
        List<GetPairVO> singlePair = pairList.stream().filter(e -> {
            List<GetPairVO> list = collect.get(e.getBaseSymbol());
            return list.size() == 1 && list.get(0).getQuoteSymbol().equalsIgnoreCase("BUSD");
        }).collect(Collectors.toList());
        String fileName = System.getProperty("user.dir") + "/币安单BUSD交易对币种(" + DateUtil.format(new Date(), "yyyy-MM-dd") + ").xlsx";
        EasyExcel.write(fileName, GetPairVO.class).sheet("币安单BUSD交易对").doWrite(singlePair);
    }

    @Test
    void getDropATH() {
        List<ATHVO> athList = new ArrayList<>();
        String url = "https://dncapi.soulbab.com/api/coin/web-coinrank?page=%d";
        for (int i = 1; i < 11; i++) {
            String format = String.format(url, i);
            String result = HttpRequest.get(format)
                    .setHttpProxy("127.0.0.1", 1080)
                    .execute().body();
            JSONObject jsonObject = JSONObject.parseObject(result);
            if (jsonObject.getInteger("code").equals(200)) {
                JSONArray jsonArray = jsonObject.getJSONArray("data");
                List<ATHVO> list = jsonArray.toJavaList(ATHVO.class);
                athList.addAll(list);
            }
        }
        List<ATHVO> collect = athList.stream().sorted(Comparator.comparing(ATHVO::getDrop_ath)).collect(Collectors.toList());
        String fileName = System.getProperty("user.dir") + "/ATH跌幅排名(" + DateUtil.format(new Date(), "yyyy-MM-dd") + ").xlsx";
        EasyExcel.write(fileName, ATHVO.class).sheet("ATH跌幅排名").doWrite(collect);
    }

}
