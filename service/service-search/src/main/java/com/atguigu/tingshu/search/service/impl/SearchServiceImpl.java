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
        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() ->
                {
                    Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfo(albumId);
                    Assert.notNull(albumInfoResult,
                            "专辑返回结果为空");
                    AlbumInfo albumInfo = albumInfoResult.getData();
                    Assert.notNull(albumInfo,
                            "专辑为空");
                    BeanUtils.copyProperties(albumInfo,
                            albumInfoIndex);
                    return albumInfo;
                },
                threadPoolExecutor);

        CompletableFuture<Void> attributeCompletableFuture = CompletableFuture.runAsync(() ->
                {
                    Result<List<AlbumAttributeValue>> albumAttributeValueResult = albumInfoFeignClient.findAlbumAttributeValue(albumId);
                    Assert.notNull(albumAttributeValueResult,
                            "专辑属性结果集为空");
                    List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueResult.getData();
                    Assert.notNull(albumAttributeValueList,
                            "专辑属性为空");
                    List<AttributeValueIndex> attributeValueList = albumAttributeValueList.stream().map(albumAttributeValue ->
                    {
                        AttributeValueIndex attributeValueIndex = new AttributeValueIndex();
                        BeanUtils.copyProperties(albumAttributeValue,
                                attributeValueIndex);
                        return attributeValueIndex;
                    }).collect(Collectors.toList());
                    albumInfoIndex.setAttributeValueIndexList(attributeValueList);

                },
                threadPoolExecutor);
        CompletableFuture<Void> categoryCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo ->
                {
                    Result<BaseCategoryView> baseCategoryViewResult = categoryFeignClient.getCategoryView(albumInfo.getCategory3Id());

                    Assert.notNull(baseCategoryViewResult,
                            "专辑分类结果集为空");
                    BaseCategoryView baseCategoryView = baseCategoryViewResult.getData();
                    Assert.notNull(baseCategoryView,
                            "专辑分类为空");
                    albumInfoIndex.setCategory1Id(baseCategoryView.getCategory1Id());
                    albumInfoIndex.setCategory2Id(baseCategoryView.getCategory2Id());
                    albumInfoIndex.setCategory3Id(baseCategoryView.getCategory3Id());
                },
                threadPoolExecutor);

        CompletableFuture<Void> userCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo ->
                {
                    Result<UserInfoVo> userInfoResult = userInfoDegradeFeignClient.getUserInfoVo(albumInfo.getUserId());
                    Assert.notNull(userInfoResult,
                            "用户结果集为空");
                    UserInfoVo userInfoVo = userInfoResult.getData();
                    Assert.notNull(userInfoVo,
                            "用户为空");

                    albumInfoIndex.setAnnouncerName(userInfoVo.getNickname());
                },
                threadPoolExecutor);
        int playStatNum = new Random().nextInt(100000);
        int subscribeStatNum = new Random().nextInt(100000000);
        int buyStatNum = new Random().nextInt(10000000);
        int commentStatNum = new Random().nextInt(1000000000);
        albumInfoIndex.setPlayStatNum(playStatNum);
        albumInfoIndex.setSubscribeStatNum(subscribeStatNum);
        albumInfoIndex.setBuyStatNum(buyStatNum);
        albumInfoIndex.setCommentStatNum(commentStatNum);
        CompletableFuture.allOf(
                albumInfoCompletableFuture,
                categoryCompletableFuture,
                attributeCompletableFuture,
                userCompletableFuture).join();
        albumInfoIndexRepository.save(albumInfoIndex);

    }
}
