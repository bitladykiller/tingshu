package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface TrackInfoService extends IService<TrackInfo> {

    Map<String, Object> uploadTrack(MultipartFile file);

    void saveTrackInfo(TrackInfoVo trackInfoVo);

    IPage<TrackListVo> findUserTrackPage(Page<TrackListVo> trackListVoPage, TrackInfoQuery trackInfoQuery);

    void removeTrackInfo(Long id);

    void updateTrackInfo(Long id,
                         TrackInfoVo trackInfoVo);

    IPage<AlbumTrackListVo> findAlbumTrackPage(Page<AlbumTrackListVo> pageParam,
                                               Long albumId,
                                               Long userId);
}
