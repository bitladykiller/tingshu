package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface AlbumInfoService extends IService<AlbumInfo> {


    void saveAlbumInfo(AlbumInfoVo albumInfoVo);

    IPage<AlbumInfoVo> selectAlbumPage(Page<AlbumInfoVo> pageParam, AlbumInfoQuery albumInfoQuery);

    void removeAlbumInfo(Long albumId);

    AlbumInfo getAlbumInfo(Long albumId);

    void updateAlbumInfo(Long albumId, AlbumInfoVo albumInfoVo);

    List<AlbumInfo> findUserAllAlbumList(Long userId);

    Map<String, Object> getAlbumInfoStat(Long albumId);

    List<AlbumAttributeValue> findAlbumAttributeValueByAlbumId(Long albumId);

    AlbumStatVo getAlbumStatVoByAlbumId(Long albumId);
}
