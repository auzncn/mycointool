package com.zjx.cointool.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.zjx.cointool.bean.WatchList;
import com.zjx.cointool.service.WatchListService;
import com.zjx.cointool.task.AlchemyTask;
import com.zjx.cointool.task.DebankTask;
import com.zjx.cointool.task.JieDataTask;
import com.zjx.cointool.task.KimchiTask;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/debank")
public class CoinController {
    @Resource
    private DebankTask debankTask;
    @Resource
    private WatchListService watchListService;
    @Resource
    private AlchemyTask alchemyTask;
    @Resource
    private KimchiTask kimchiTask;
    @Resource
    private JieDataTask jieDataTask;

    @GetMapping("get_list")
    public void getList() throws InterruptedException {
        debankTask.scanWatchList();
    }

    @GetMapping("get_watch_list")
    public Object get_watch_list() {
        return watchListService.list();
    }

    @PostMapping("add_watch_list")
    public void add_watch_list(@RequestBody List<WatchList> list) {
        List<WatchList> addList = new ArrayList<>();
        list.forEach(e -> {
            WatchList byId = watchListService.getById(e.getId());
            if (byId == null) {
                addList.add(e);
            }
        });
        if (CollectionUtil.isNotEmpty(addList)) {
            watchListService.saveBatch(addList);
        }
    }

    @GetMapping("scan_watch_list")
    public void scan_watch_list() throws InterruptedException {
        alchemyTask.scanWatchList();
    }

    @GetMapping("kimchi_premium")
    public void kimchi_premium() throws InterruptedException {
        kimchiTask.kimchiPremium();
    }

    @GetMapping("checkOutflow")
    public void checkOutflow() throws InterruptedException {
        jieDataTask.checkOutflow();
    }
}
