package com.zjx.cointool.util;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;

import java.util.HashMap;

public class DingUtil {
    public static void ding(String message) {
        String webhook = "webhoolk";

        HashMap<String, Object> paramMap = new HashMap<>();
        HashMap<String, Object> textMap = new HashMap<>();
        textMap.put("content", message);
        paramMap.put("msgtype", "text");
        paramMap.put("text", textMap);
        String result = HttpUtil.post(webhook, JSON.toJSONString(paramMap));
    }
}
