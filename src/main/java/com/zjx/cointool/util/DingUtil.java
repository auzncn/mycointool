package com.zjx.cointool.util;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class DingUtil {
    @Value("${ding-webhook}")
    private String webhook;

    public void ding(String message) {

        HashMap<String, Object> paramMap = new HashMap<>();
        HashMap<String, Object> textMap = new HashMap<>();
        textMap.put("content", message);
        paramMap.put("msgtype", "text");
        paramMap.put("text", textMap);
        HttpUtil.post(webhook, JSON.toJSONString(paramMap));
    }


}
