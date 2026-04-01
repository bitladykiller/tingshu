package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "声音管理")
@RestController
@RequestMapping("api/album/trackInfo")
@SuppressWarnings({"all"})
public class TrackInfoApiController
{

    @Autowired
    private TrackInfoService trackInfoService;
    @Autowired
    private VodService vodService;


    @Operation(summary = "上传声音的文件")
    @PostMapping("uploadTrack")
    public Result<Map<String, Object>> uploadTrack(MultipartFile file)
    {
        Map<String, Object> map = vodService.uploadTrack(file);
        return Result.ok(map);
    }

    @Operation(summary = "新增声音")
    @PostMapping("saveTrackInfo")
    public Result saveTrackInfo(@RequestBody @Validated TrackInfoVo trackInfoVo)
    {
        trackInfoService.saveTrackInfo(trackInfoVo);
        return Result.ok();
    }

    @Operation(summary = "获取当前用户声音分页列表")
    @PostMapping("findUserTrackPage/{page}/{limit}")
    public Result<IPage<TrackListVo>> findUserTrackPage(@Parameter(name = "page", description = "当前页面", required = true)
                                                        @PathVariable Long page,
                                                        @Parameter(name = "limit", description = "每页记录数", required = true)
                                                        @PathVariable Long limit,
                                                        @Parameter(name = "trackInfoQuery", description = "查询对象", required = false)
                                                        @RequestBody TrackInfoQuery trackInfoQuery)
    {
        trackInfoQuery.setUserId(AuthContextHolder.getUserId());
        Page<TrackListVo> trackListVoPage = new Page<>(page,
                limit);
        IPage<TrackListVo> trackListVoIPage = trackInfoService.findUserTrackPage(trackListVoPage,
                trackInfoQuery);
        return Result.ok(trackListVoIPage);
    }

    @Operation(summary = "删除声音信息")
    @DeleteMapping("removeTrackInfo/{id}")
    public Result removeTrackInfo(@PathVariable Long id)
    {
        //	调用服务层方法
        trackInfoService.removeTrackInfo(id);
        return Result.ok();
    }

    @Operation(summary = "获取声音信息")
    @GetMapping("getTrackInfo/{id}")
    public Result<TrackInfo> getTrackInfo(@PathVariable Long id)
    {
        TrackInfo trackInfo = trackInfoService.getById(id);
        return Result.ok(trackInfo);
    }

    @Operation(summary = "修改声音")
    @PutMapping("updateTrackInfo/{id}")
    public Result updateById(@PathVariable Long id,
                             @RequestBody @Validated TrackInfoVo trackInfoVo)
    {
        trackInfoService.updateTrackInfo(id,
                trackInfoVo);
        return Result.ok();
    }

    @GuiGuLogin(required = false)
    @Operation(summary = "获取专辑声音分页列表")
    @GetMapping("findAlbumTrackPage/{albumId}/{page}/{limit}")
    public Result<IPage<AlbumTrackListVo>> findAlbumTrackPage(
            @Parameter(name = "albumId", description = "专辑id", required = true)
            @PathVariable Long albumId,
            @Parameter(name = "page", description = "当前页码", required = true)
            @PathVariable Long page,
            @Parameter(name = "limit", description = "每页记录数", required = true)
            @PathVariable Long limit)
    {
        Long userId = AuthContextHolder.getUserId();
        Page<AlbumTrackListVo> pageParam = new Page<>(page,
                limit);
        IPage<AlbumTrackListVo> pageModel = trackInfoService.findAlbumTrackPage(pageParam,
                albumId,
                userId);
        return Result.ok(pageModel);
    }

}

