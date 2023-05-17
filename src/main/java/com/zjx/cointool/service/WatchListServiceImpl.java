package com.zjx.cointool.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjx.cointool.bean.WatchList;
import com.zjx.cointool.dao.WatchListDao;
import org.springframework.stereotype.Service;

@Service
public class WatchListServiceImpl extends ServiceImpl<WatchListDao, WatchList> implements WatchListService {

}
