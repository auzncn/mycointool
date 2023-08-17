package com.zjx.cointool.task;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zjx.cointool.util.DingUtil;
import com.zjx.cointool.vo.jiedata.InOutFlowVO;
import com.zjx.cointool.vo.jiedata.NetOutflowVO;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JieDataTask {
    private static final String IN_OUT_FLOW_URL = "https://jiedata.com/api/home/ex-from-to";

    @Resource
    private DingUtil dingUtil;

    @Scheduled(cron = "0 0 23 * * ?")
    public void checkOutflow() {
        Map<String, Object> params = new HashMap<>();
        Map<String, Object> args = new HashMap<>();

        args.put("field", "to");
        args.put("day", 7);
        args.put("limit", 50);
        args.put("offset", 0);
        params.put("args", args);
        String toResult = HttpRequest.post(IN_OUT_FLOW_URL)
                .setHttpProxy("127.0.0.1", 1080)
                .body(JSON.toJSONString(params))
                .execute().body();
        //流出
        JSONObject toObject = JSON.parseObject(toResult);
        List<InOutFlowVO> toItems = toObject.getJSONArray("items").toJavaList(InOutFlowVO.class);

        args.put("field", "from");
        String fromResult = HttpRequest.post(IN_OUT_FLOW_URL)
                .setHttpProxy("127.0.0.1", 1080)
                .body(JSON.toJSONString(params))
                .execute().body();
        //流入
        JSONObject fromObject = JSON.parseObject(fromResult);
        List<InOutFlowVO> fromItems = fromObject.getJSONArray("items").toJavaList(InOutFlowVO.class);

        Map<String, InOutFlowVO> fromMap = fromItems.stream()
                .collect(Collectors.toMap(InOutFlowVO::getToken_addr, p -> p, (v1, v2) -> v2));

        List<NetOutflowVO> outList = new ArrayList<>();
        for (InOutFlowVO e : toItems) {
            BigDecimal outUsd = new BigDecimal(e.getBalance_usd());
            InOutFlowVO inVO = fromMap.get(e.getToken_addr());
            if (inVO == null) {
                NetOutflowVO vo = new NetOutflowVO();
                vo.setSymbol(e.getSymbol());
                vo.setNetOutUsd(outUsd);
                outList.add(vo);
            } else {
                BigDecimal inUsd = new BigDecimal(inVO.getBalance_usd());
                if (outUsd.compareTo(inUsd) > 0) {
                    NetOutflowVO vo = new NetOutflowVO();
                    vo.setSymbol(e.getSymbol());
                    vo.setNetOutUsd(outUsd.subtract(inUsd));
                    outList.add(vo);
                }
            }
        }
        if (CollUtil.isNotEmpty(outList)) {
            List<NetOutflowVO> collect = outList.stream()
                    .sorted(Comparator.comparing(NetOutflowVO::getNetOutUsd).reversed())
                    .collect(Collectors.toList());
            StringBuilder msg = new StringBuilder(DateUtil.now() + " CEX 7日净流出查询\n");
            collect.forEach(e -> {
                msg.append(e.generateMsg());
            });
            System.out.println(msg);
            dingUtil.ding(msg.toString());
        }
    }
}
