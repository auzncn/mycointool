package com.zjx.cointool.task;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.excel.util.StringUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zjx.cointool.bean.WatchList;
import com.zjx.cointool.service.WatchListService;
import com.zjx.cointool.util.DingUtil;
import com.zjx.cointool.vo.debank.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DebankTask {
    public static String LIST_URL = "https://api.debank.com/history/list?user_addr=%s&chain=&start_time=0&page_count=20";
    public static String CHAIN_URL = "https://api.debank.com/chain/list";
    public static String BALANCE_URL = "https://api.debank.com/user?id=%s";
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private WatchListService watchListService;
    @Resource
    private DingUtil dingUtil;

    //    @Scheduled(cron = "0 */10 * * * ?")
    public void scanWatchList() throws InterruptedException {
        List<WatchList> list = watchListService.list(new QueryWrapper<WatchList>().eq("active", 1));
        for (WatchList watchList : list) {
            String listFormat = String.format(LIST_URL, watchList.getId());
            HttpRequest listRequest = HttpRequest.get(listFormat);
            setHeader(listRequest);
            String listResult = listRequest.execute().body();
            System.out.println(listResult);
            JSONObject jsonObject = JSONObject.parseObject(listResult);

            Thread.sleep(3300);
            String balanceFormat = String.format(BALANCE_URL, watchList.getId());
            HttpRequest balanceRequest = HttpRequest.get(balanceFormat);
            setHeader(balanceRequest);
            String balanceResult = balanceRequest.execute().body();
            System.out.println(balanceResult);
            JSONObject balanceObj = JSONObject.parseObject(balanceResult);

            HttpRequest chainRequest = HttpRequest.get(CHAIN_URL);
            setHeader(chainRequest);
            String chainResult = chainRequest.execute().body();
            System.out.println(chainResult);
            JSONObject chainObj = JSONObject.parseObject(chainResult);
            Thread.sleep(3300);

            try {

                if (jsonObject.getIntValue("error_code") == 0) {
                    JSONObject data = jsonObject.getJSONObject("data");
                    JSONObject token_dict = data.getJSONObject("token_dict");
                    JSONObject project_dict = data.getJSONObject("project_dict");
                    JSONArray jsonArray = data.getJSONArray("history_list");
                    if (jsonArray != null) {
                        String s = stringRedisTemplate.opsForValue().get(watchList.getId());
                        if (StringUtils.isBlank(s)) {
                            stringRedisTemplate.opsForValue().set(watchList.getId(), jsonArray.toString());
                        } else {
                            List<History_list> newList = jsonArray.toJavaList(History_list.class);
                            List<History_list> oldList = JSON.parseArray(s, History_list.class);
                            List<History_list> noticeList = new ArrayList<>();
                            boolean updateCache = false;
                            for (History_list h1 : newList) {
                                String id = h1.getId();
                                List<String> oldListIds = oldList.stream().map(History_list::getId).collect(Collectors.toList());
                                if (!oldListIds.contains(id)) {
                                    noticeList.add(h1);
                                    updateCache = true;
                                }
                            }
                            if (updateCache) {
                                for (History_list n : noticeList) {
                                    if (!n.is_scam()) {
                                        List<String> l = new ArrayList<>();
                                        Date date = new Date(n.getTime_at() * 1000);
                                        //交易时间
                                        String txTime = DateUtil.formatDateTime(date);
                                        l.add(txTime);
                                        String usdValue = balanceObj.getJSONObject("data").getJSONObject("user").getJSONObject("desc").getString("usd_value");
                                        l.add(watchList.getTag());
                                        l.add("【" + watchList.getId() + "】");
                                        l.add("钱包价值 $" + usdValue);
                                        if (n.getCex_id() != null) {
                                            boolean receive = "receive".equals(n.getCate_id());
                                            boolean send = "send".equals(n.getCate_id());
                                            if (receive) {
                                                l.add("交易所提款" + n.getCex_id());
                                            }
                                            if (send) {
                                                l.add("发送到交易所" + n.getCex_id());
                                            }
                                        }
                                        Tx tx = n.getTx();
                                        String project_id = n.getProject_id();
                                        if (project_id != null) {
                                            String projectName = project_dict.getJSONObject(project_id).getString("name");
                                            l.add("使用" + projectName);
                                        }
                                        //方法名称
                                        String name = tx.getName();
                                        if (StringUtils.isNotBlank(name)) {
                                            l.add("方法名称" + name);
                                        }

                                        JSONArray chains = chainObj.getJSONObject("data").getJSONArray("chains");
                                        List<Chain> chainList = chains.toJavaList(Chain.class);
                                        Map<String, Chain> chainMap = chainList.stream().collect(Collectors.toMap(Chain::getId, p -> p));

                                        //tx
                                        Double value = tx.getValue();
                                        if (value != null && value > 0) {
                                            boolean to = tx.getTo_addr().equals(watchList.getId());
                                            if (chainMap.get(n.getChain()) != null) {
                                                Chain chain = chainMap.get(n.getChain());
                                                String token_symbol = chain.getToken_symbol();
                                                if (to) {
                                                    l.add("接收" + value + token_symbol);
                                                } else {
                                                    l.add("转出" + value + token_symbol);
                                                }
                                            }
                                        }
                                        //发送的代币
                                        if (CollectionUtil.isNotEmpty(n.getSends())) {
                                            StringBuilder sendString = new StringBuilder("转出");
                                            for (Sends send : n.getSends()) {
                                                String amount = send.getAmount();
                                                String token_id = send.getToken_id();
                                                String price = send.getPrice();
                                                String symbol = token_dict.getJSONObject(token_id).getString("symbol");
                                                StringBuilder append = sendString.append(symbol).append(",数量").append(amount);
                                                if (StringUtils.isNotBlank(price)) {
                                                    append.append(",价格").append(price);
                                                }
                                                l.add(sendString.toString());
                                            }
                                        }

                                        //接收到的代币
                                        if (CollectionUtil.isNotEmpty(n.getReceives())) {
                                            StringBuilder receiveString = new StringBuilder("收到");
                                            for (Receives receive : n.getReceives()) {
                                                String amount = receive.getAmount();
                                                String token_id = receive.getToken_id();
                                                String price = receive.getPrice();
                                                String symbol = token_dict.getJSONObject(token_id).getString("symbol");
                                                StringBuilder append = receiveString.append(symbol).append(",数量").append(amount);
                                                if (StringUtils.isNotBlank(price)) {
                                                    append.append(",价格").append(price);
                                                }
                                                l.add(receiveString.toString());
                                            }
                                        }


                                        if (chainMap.get(n.getChain()) != null) {
                                            Chain chain = chainMap.get(n.getChain());
                                            l.add("查看交易" + chain.getExplorer_host() + "/tx/" + n.getId());
                                        }
                                        String join = String.join("\n", l);
                                        System.out.println(join);
                                        dingUtil.ding(join);
                                    }
                                }
                                stringRedisTemplate.opsForValue().set(watchList.getId(), JSONObject.toJSONString(newList));
                            }
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private HttpRequest setHeader(HttpRequest request) {
        String time = String.valueOf((int) (System.currentTimeMillis() / 1200));
        String randomId = UUID.randomUUID().toString().replace("-", "");
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        String randomString = RandomUtil.randomString(40);
        String sign = RandomUtil.randomString(64);
//        String account = "{\"random_at\":%s,\"random_id\":\"%s\",\"session_id\":\"%s\",\"user_addr\":\"%s\",\"wallet_type\":\"metamask\",\"is_verified\":true}";
        String account = "{\"random_at\":%s,\"random_id\":\"%s\",\"user_addr\":null}";
        String secchua = "\"Chromium\";v=\"%d\", \"Microsoft Edge\";v=\"%d\", \"Not:A-Brand\";v=\"99\"";
        int i = RandomUtil.randomInt(95, 112);
        request.setHttpProxy("127.0.0.1", 1080)
                .header("authority", "api.debank.com")
                .header("accept", "*/*")
                .header("accept-language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                .header("account", String.format(account, time, randomId))
//                .header("cache-control", "no-cache")
                .header("origin", "https://debank.com")
                .header("pragma", "no-cache")
                .header("referer", "https://debank.com/")
                .header("sec-ch-ua", String.format(secchua, i, i))
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"Windows\"")
                .header("sec-fetch-dest", "empty")
                .header("sec-fetch-mode", "cors")
                .header("sec-fetch-site", "same-site")
                .header("source", "web")
                .header("x-api-ts", time)
                .header("x-api-ver", "v2")
//                .header("Connection", "keep-alive")
                .header("Accept-Encoding", "gzip, deflate, br");
        return setUserAgent(request);
    }

    private HttpRequest setUserAgent(HttpRequest request) {
        List<String> list = new ArrayList<>();
        list.add("Mozilla/5.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/5.1)");
        list.add("Mozilla/5.0 (Windows; U; Windows CE) AppleWebKit/533.37.3 (KHTML, like Gecko) Version/5.1 Safari/533.37.3");
        list.add("Mozilla/5.0 (Windows; U; Windows NT 6.2) AppleWebKit/535.46.6 (KHTML, like Gecko) Version/4.1 Safari/535.46.6");
        list.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows 98; Win 9x 4.90; Trident/4.0)");
        list.add("Mozilla/5.0 (compatible; MSIE 6.0; Windows NT 6.1; Trident/3.1)");
        list.add("Mozilla/5.0 (Windows NT 5.0; or-IN; rv:1.9.0.20) Gecko/2013-09-07 20:46:37 Firefox/8.0");
        list.add("Opera/8.35.(Windows NT 6.0; zu-ZA) Presto/2.9.172 Version/11.00");
        list.add("Mozilla/5.0 (Windows NT 4.0; nds-DE; rv:1.9.2.20) Gecko/2011-03-05 13:13:14 Firefox/3.8");
        list.add("Mozilla/5.0 (compatible; MSIE 8.0; Windows 95; Trident/5.1)");
        list.add("Mozilla/5.0 (compatible; MSIE 5.0; Windows NT 5.0; Trident/4.0)");
        int i = RandomUtil.randomInt(0, 9);
        request.header("user-agent", list.get(i));
        return request;
    }


    public static void main(String[] args) {
        String time = String.valueOf((int) (System.currentTimeMillis() / 1000));
        System.out.println(time);
    }
}
