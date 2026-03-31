package com.atguigu.tingshu.search.service.impl;

import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.CategoryFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.search.repository.AlbumInfoIndexRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.impl.UserInfoDegradeFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


@Slf4j
@Service
@SuppressWarnings({"all"})
public class SearchServiceImpl implements SearchService
{


    @Autowired
    private AlbumInfoIndexRepository albumInfoIndexRepository;

    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Autowired
    private CategoryFeignClient categoryFeignClient;
    @Autowired
    private UserInfoDegradeFeignClient userInfoDegradeFeignClient;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public void upperAlbum(Long albumId)
    {

        CompletableFuture<AlbumInfo> albumFuture = CompletableFuture.supplyAsync(() ->
                {
                    Result<AlbumInfo> result = albumInfoFeignClient.getAlbumInfo(albumId);
                    Assert.notNull(result,
                            "专辑返回结果为空");
                    Assert.notNull(result.getData(),
                            "专辑为空");
                    return result.getData();
                },
                threadPoolExecutor);

        CompletableFuture<List<AttributeValueIndex>> attrFuture = CompletableFuture.supplyAsync(() ->
                {
                    Result<List<AlbumAttributeValue>> result = albumInfoFeignClient.findAlbumAttributeValue(albumId);
                    Assert.notNull(result,
                            "专辑属性结果集为空");
                    Assert.notNull(result.getData(),
                            "专辑属性为空");

                    return result.getData().stream().map(item ->
                    {
                        AttributeValueIndex index = new AttributeValueIndex();

                        BeanUtils.copyProperties(item,
                                index);
                        return index;
                    }).collect(Collectors.toList());
                },
                threadPoolExecutor);

        CompletableFuture<BaseCategoryView> categoryFuture = albumFuture.thenApplyAsync(albumInfo ->
                {
                    Result<BaseCategoryView> result = categoryFeignClient.getCategoryView(albumInfo.getCategory3Id());
                    Assert.notNull(result,
                            "专辑分类结果集为空");
                    Assert.notNull(result.getData(),
                            "专辑分类为空");
                    return result.getData();
                },
                threadPoolExecutor);

        CompletableFuture<UserInfoVo> userFuture = albumFuture.thenApplyAsync(albumInfo ->
                {
                    Result<UserInfoVo> result = userInfoDegradeFeignClient.getUserInfoVo(albumInfo.getUserId());
                    Assert.notNull(result,
                            "用户结果集为空");
                    Assert.notNull(result.getData(),
                            "用户为空");
                    return result.getData();
                },
                threadPoolExecutor);

        CompletableFuture.allOf(albumFuture,
                attrFuture,
                categoryFuture,
                userFuture).join();

        AlbumInfo albumInfo = albumFuture.join();
        List<AttributeValueIndex> attrList = attrFuture.join();
        BaseCategoryView categoryView = categoryFuture.join();
        UserInfoVo userInfoVo = userFuture.join();

        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();

        BeanUtils.copyProperties(albumInfo,
                albumInfoIndex);

        albumInfoIndex.setAttributeValueIndexList(attrList);

        albumInfoIndex.setCategory1Id(categoryView.getCategory1Id());
        albumInfoIndex.setCategory2Id(categoryView.getCategory2Id());
        albumInfoIndex.setCategory3Id(categoryView.getCategory3Id());

        albumInfoIndex.setAnnouncerName(userInfoVo.getNickname());

        albumInfoIndex.setPlayStatNum(ThreadLocalRandom.current().nextInt(100000));
        albumInfoIndex.setSubscribeStatNum(ThreadLocalRandom.current().nextInt(100000000));
        albumInfoIndex.setBuyStatNum(ThreadLocalRandom.current().nextInt(10000000));
        albumInfoIndex.setCommentStatNum(ThreadLocalRandom.current().nextInt(1000000000));

        albumInfoIndexRepository.save(albumInfoIndex);
    }
}
