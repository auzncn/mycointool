package com.zjx.cointool.task;

import cn.hutool.http.HttpRequest;
import com.alibaba.excel.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zjx.cointool.bean.WatchList;
import com.zjx.cointool.service.WatchListService;
import com.zjx.cointool.util.DingUtil;
import com.zjx.cointool.vo.alchemy.AlchemyResVO;
import com.zjx.cointool.vo.alchemy.TransactionVO;
import lombok.SneakyThrows;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AlchemyTask {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private WatchListService watchListService;
    @Value("${alchemy-key}")
    private String key;

    private static final String url = "https://%s.g.alchemy.com/v2/%s";
    private static final String[] chains = new String[]{"eth-mainnet", "arb-mainnet"};
    private static final String txInfo = "https://explorer.phalcon.xyz/tx/%s/%s";

    @Scheduled(cron = "0 0/5 * * * ?")
    public void scanWatchList() throws InterruptedException {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "1");
        map.put("jsonrpc", "2.0");
        map.put("method", "alchemy_getAssetTransfers");
        Map<String, Object> params = new HashMap<>();
        params.put("fromBlock", "0x0");
        params.put("toBlock", "latest");
        params.put("withMetadata", true);
        params.put("excludeZeroValue", true);
        params.put("maxCount", "0xA");
        params.put("category", Arrays.asList("external", "erc20", "internal"));
        params.put("order", "desc");

        List<WatchList> watchList = watchListService.list(new QueryWrapper<WatchList>().eq("active", 1));
        for (WatchList w : watchList) {
            for (String chain : chains) {
                params.remove("toAddress");
                params.remove("fromAddress");
                if (!chain.equals("eth-mainnet")) {
                    params.put("category", Arrays.asList("erc20", "external"));
                }

                String formatUrl = String.format(url, chain, key);
                params.put("toAddress", w.getId());
                map.put("params", params);
                String s1 = JSONObject.toJSONString(map);
                long stime = System.currentTimeMillis();
                String receiveResult = HttpRequest.post(formatUrl)
                        .setHttpProxy("127.0.0.1", 1080)
                        .header("accept", "application/json")
                        .header("content-type", "application/json")
                        .body(s1)
                        .timeout(20000)
                        .execute().body();
                long etime = System.currentTimeMillis();
                System.out.printf("网络请求时长：%d 毫秒.", (etime - stime));
                JSONObject receiveJson = JSONObject.parseObject(receiveResult);
                List<AlchemyResVO> receiveResList = receiveJson.getJSONObject("result").getJSONArray("transfers").toJavaList(AlchemyResVO.class);
                Map<String, List<AlchemyResVO>> receiveMap = receiveResList.stream().collect(Collectors.groupingBy(AlchemyResVO::getHash));

                Thread.sleep(1000);

                params.remove("toAddress");
                params.put("fromAddress", w.getId());
                map.put("params", params);
                String s2 = JSONObject.toJSONString(map);
                String sendResult = HttpRequest.post(formatUrl)
                        .setHttpProxy("127.0.0.1", 1080)
                        .header("accept", "application/json")
                        .header("content-type", "application/json")
                        .body(s2)
                        .timeout(20000)
                        .execute().body();

                JSONObject sendJson = JSONObject.parseObject(sendResult);
                List<AlchemyResVO> sendResList = sendJson.getJSONObject("result").getJSONArray("transfers").toJavaList(AlchemyResVO.class);
                Map<String, List<AlchemyResVO>> sendMap = sendResList.stream().collect(Collectors.groupingBy(AlchemyResVO::getHash));

                List<TransactionVO> list = new ArrayList<>();
                sendMap.keySet().forEach(hash -> {
                    TransactionVO transactionVO = new TransactionVO();
                    List<AlchemyResVO> sameHashSendList = sendMap.get(hash);
                    transactionVO.setHash(hash);
                    transactionVO.setTime(sameHashSendList.get(0).getMetadata().getBlockTimestamp());
                    transactionVO.setUserId(w.getId());
                    transactionVO.setTag(w.getTag());
                    Map<String, TransactionVO.SendReceive> sendAddressMap = new HashMap<>();
                    sameHashSendList.forEach(a -> {
                        TransactionVO.SendReceive sendReceive = sendAddressMap.get(a.getRawContract().getAddress());
                        if (sendReceive == null) {
                            sendReceive = new TransactionVO.SendReceive();
                            BeanUtils.copyProperties(a, sendReceive);
                            sendReceive.setSendTo(a.getTo());
                        } else {
                            sendReceive.setValue(a.getValue().add(sendReceive.getValue()));
                        }
                        sendAddressMap.put(a.getRawContract().getAddress(), sendReceive);
                    });
                    transactionVO.setSend(new ArrayList<>(sendAddressMap.values()));
                    Map<String, TransactionVO.SendReceive> receiveAddressMap = new HashMap<>();
                    if (receiveMap.get(hash) != null) {
                        List<AlchemyResVO> sameHashReceiveList = receiveMap.get(hash);
                        sameHashReceiveList.forEach(a -> {
                            TransactionVO.SendReceive sendReceive = receiveAddressMap.get(a.getRawContract().getAddress());
                            if (sendReceive == null) {
                                sendReceive = new TransactionVO.SendReceive();
                                BeanUtils.copyProperties(a, sendReceive);
                                sendReceive.setReceiveFrom(a.getFrom());
                            } else {
                                sendReceive.setValue(a.getValue().add(sendReceive.getValue()));
                            }
                            receiveAddressMap.put(a.getRawContract().getAddress(), sendReceive);
                        });
                    }
                    transactionVO.setReceive(new ArrayList<>(receiveAddressMap.values()));
                    list.add(transactionVO);
                });

                receiveMap.keySet().forEach(hash -> {
                    Optional<TransactionVO> any = list.stream().filter(e -> e.getHash().equals(hash)).findAny();
                    if (!any.isPresent()) {
                        TransactionVO transactionVO = new TransactionVO();
                        List<AlchemyResVO> sameHashReceiveList = receiveMap.get(hash);
                        transactionVO.setHash(hash);
                        transactionVO.setTime(sameHashReceiveList.get(0).getMetadata().getBlockTimestamp());
                        transactionVO.setUserId(w.getId());
                        transactionVO.setTag(w.getTag());
                        Map<String, TransactionVO.SendReceive> receiveAddressMap = new HashMap<>();
                        sameHashReceiveList.forEach(a -> {
                            TransactionVO.SendReceive sendReceive = receiveAddressMap.get(a.getRawContract().getAddress());
                            if (sendReceive == null) {
                                sendReceive = new TransactionVO.SendReceive();
                                BeanUtils.copyProperties(a, sendReceive);
                                sendReceive.setReceiveFrom(a.getFrom());
                            } else {
                                sendReceive.setValue(a.getValue().add(sendReceive.getValue()));
                            }
                            receiveAddressMap.put(a.getRawContract().getAddress(), sendReceive);
                        });
                        transactionVO.setReceive(new ArrayList<>(receiveAddressMap.values()));
                        list.add(transactionVO);
                    }
                });
                List<TransactionVO> newList = list.stream().sorted(Comparator.comparing(TransactionVO::getTime).reversed()).collect(Collectors.toList());
                String s = stringRedisTemplate.opsForValue().get(chain + ":" + w.getId());
                if (StringUtils.isBlank(s)) {
                    stringRedisTemplate.opsForValue().set(chain + ":" + w.getId(), JSONObject.toJSONString(newList));
                } else {
                    List<TransactionVO> cacheList = JSON.parseArray(s, TransactionVO.class);
                    List<TransactionVO> noticeList = new ArrayList<>();
                    boolean updateCache = false;
                    for (TransactionVO transaction : newList) {
                        String hash = transaction.getHash();
                        List<String> oldHashList = cacheList.stream().map(TransactionVO::getHash).collect(Collectors.toList());
                        if (!oldHashList.contains(hash)) {
                            noticeList.add(transaction);
                            updateCache = true;
                        }
                    }
                    if (updateCache) {
                        stringRedisTemplate.opsForValue().set(chain + ":" + w.getId(), JSONObject.toJSONString(newList));
                        for (TransactionVO noticeTx : noticeList) {
                            List<String> msgList = noticeTx.generateMsg();
                            String info = "";
                            if (chain.equals("eth-mainnet")) {
                                info = String.format(txInfo, "eth", noticeTx.getHash());
                            } else if (chain.equals("arb-mainnet")) {
                                info = String.format(txInfo, "arbitrum", noticeTx.getHash());
                            }
                            msgList.add(info);
                            String msg = String.join("\n\n", msgList);
                            DingUtil.ding(msg);
                        }
                    }
                }
            }
        }
    }
}
