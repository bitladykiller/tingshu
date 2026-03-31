package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.AlbumStat;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface AlbumStatMapper extends BaseMapper<AlbumStat>
{


    Map<String, Object> getAlbumInfoStat(@Param("id") Long albumId);
}
