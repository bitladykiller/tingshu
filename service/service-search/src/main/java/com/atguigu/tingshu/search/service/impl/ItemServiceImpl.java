package com.atguigu.tingshu.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.client.*;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.search.service.ItemService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Service
public class ItemServiceImpl implements ItemService
{

    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Autowired
    private CategoryFeignClient categoryFeignClient;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public Map<String, Object> getItem(Long albumId)
    {
        CompletableFuture<AlbumInfo> albumFuture = CompletableFuture.supplyAsync(() ->
                {
                    Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfo(albumId);
                    Assert.notNull(albumInfoResult,
                            "得到的 albumInfoResult 为空");
                    AlbumInfo albumInfo = albumInfoResult.getData();
                    Assert.notNull(albumInfo,
                            "得到的 albumInfo 为空");
                    log.info("albumInfo:{}",
                            JSON.toJSONString(albumInfo));
                    return albumInfo;
                },
                threadPoolExecutor);

        CompletableFuture<AlbumStatVo> albumStatFuture = CompletableFuture.supplyAsync(() ->
                {
                    Result<AlbumStatVo> albumStatVoResult = albumInfoFeignClient.getAlbumStatVo(albumId);
                    AlbumStatVo albumStatVo = albumStatVoResult.getData();
                    log.info("albumStatVo:{}",
                            JSON.toJSONString(albumStatVo));
                    return albumStatVo;
                },
                threadPoolExecutor);

        CompletableFuture<BaseCategoryView> categoryFuture = albumFuture.thenApplyAsync(albumInfo ->
                {
                    Result<BaseCategoryView> baseCategoryViewResult =
                            categoryFeignClient.getCategoryView(albumInfo.getCategory3Id());
                    BaseCategoryView baseCategoryView = baseCategoryViewResult.getData();
                    log.info("baseCategoryView:{}",
                            JSON.toJSONString(baseCategoryView));
                    return baseCategoryView;
                },
                threadPoolExecutor);

        CompletableFuture<UserInfoVo> announcerFuture = albumFuture.thenApplyAsync(albumInfo ->
                {
                    Result<UserInfoVo> userInfoVoResult =
                            userInfoFeignClient.getUserInfoVo(albumInfo.getUserId());
                    UserInfoVo userInfoVo = userInfoVoResult.getData();
                    log.info("announcer:{}",
                            JSON.toJSONString(userInfoVo));
                    return userInfoVo;
                },
                threadPoolExecutor);

        CompletableFuture.allOf(albumFuture,
                albumStatFuture,
                categoryFuture,
                announcerFuture).join();

        Map<String, Object> result = new HashMap<>(4);
        result.put("albumInfo",
                albumFuture.join());
        result.put("albumStatVo",
                albumStatFuture.join());
        result.put("baseCategoryView",
                categoryFuture.join());
        result.put("announcer",
                announcerFuture.join());
        return result;
    }
}
