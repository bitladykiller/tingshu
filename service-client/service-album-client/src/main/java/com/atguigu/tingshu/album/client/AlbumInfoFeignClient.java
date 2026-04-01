package com.atguigu.tingshu.album.client;

import com.atguigu.tingshu.album.client.impl.AlbumInfoDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;


@FeignClient(value = "service-album", fallback = AlbumInfoDegradeFeignClient.class)
public interface AlbumInfoFeignClient
{
    @GetMapping("api/album/albumInfo/findAlbumAttributeValue/{albumId}")
    Result<List<AlbumAttributeValue>> findAlbumAttributeValue(@PathVariable("albumId") Long albumId);

    @GetMapping("api/album/albumInfo/getAlbumInfo/{id}")
    Result<AlbumInfo> getAlbumInfo(@PathVariable("id") Long id);

    @GetMapping("api/album/albumInfo/getAlbumStatVo/{albumId}")
    Result<AlbumStatVo> getAlbumStatVo(@PathVariable Long albumId);


}