package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface VodService {

    Map<String, Object> uploadTrack(MultipartFile file);

    TrackMediaInfoVo getmediaInfoByFileId(@NotEmpty(message = "媒体文件Id不能为空") String mediaFileId);

    void removeTrack(String mediaFileId);
}
