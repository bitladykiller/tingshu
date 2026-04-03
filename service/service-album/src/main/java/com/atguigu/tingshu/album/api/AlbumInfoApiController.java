package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencentcloudapi.scf.v20180416.models.PublishVersionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album/albumInfo")
@SuppressWarnings({"all"})
public class AlbumInfoApiController
{

    @Autowired
    private AlbumInfoService albumInfoService;
    @Autowired
    private RedissonClient redissonClient;

    @Operation(summary = "保存提交的专辑信息")
    @PostMapping("saveAlbumInfo")
    public Result saveAlbumInfo(@RequestBody @Validated AlbumInfoVo albumInfoVo)
    {
        albumInfoService.saveAlbumInfo(albumInfoVo);
        return Result.ok();
    }

    @Operation(summary = "分页查询专辑的信息")
    @PostMapping("findUserAlbumPage/{page}/{limit}")
    public Result findUserAlbumPage(@PathVariable Long page,
                                    @PathVariable Long limit,
                                    @RequestBody(required = false) AlbumInfoQuery albumInfoQuery)
    {
        Page<AlbumInfoVo> pageParam = new Page<>(page,
                limit);
        IPage<AlbumInfoVo> pageModel = albumInfoService.selectAlbumPage(pageParam,
                albumInfoQuery);
        return Result.ok(pageModel);
    }

    @Operation(summary = "删除专辑信息")
    @DeleteMapping("removeAlbumInfo/{albumId}")
    public Result removeAlbumInfo(@PathVariable Long albumId)
    {
        albumInfoService.removeAlbumInfo(albumId);
        return Result.ok();
    }

    @Operation(summary = "查询专辑信息")
    @GetMapping("getAlbumInfo/{albumId}")
    public Result getAlbumInfo(@PathVariable Long albumId)
    {
        AlbumInfo albumInfo = albumInfoService.getAlbumInfo(albumId);
        return Result.ok(albumInfo);
    }

    @Operation(summary = "更新专辑信息")
    @PutMapping("updateAlbumInfo/{albumID}")
    public Result updateAlbumInfo(@PathVariable Long albumId,
                                  @RequestBody @Validated AlbumInfoVo albumInfoVo)
    {
        albumInfoService.updateAlbumInfo(albumId,
                albumInfoVo);
        return Result.ok();
    }

    @GetMapping("findUserAllAlbumList")
    @Operation(summary = "查询用户所有的专辑")
    public Result findUserAllAlbumList()
    {
        // TODO 后续完善登录的功能可以显示传递用户id
        Long userId = 1L;
        List<AlbumInfo> albumInfoList = albumInfoService.findUserAllAlbumList(userId);
        return Result.ok(albumInfoList);
    }

    @Operation(summary = "根据专辑的ID查询相关的专辑的状态")
    @GetMapping("getAlbumInfoStat/{albumId}")
    public Result getAlbumInfoStat(@PathVariable Long albumId)
    {
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        if(!bloomFilter.contains(albumId))
            throw new GuiguException(228,"专辑不存在{}"+albumId);
        Map<String, Object> stat = albumInfoService.getAlbumInfoStat(albumId);
        return Result.ok(stat);
    }

    @Operation(summary = "获取专辑属性值列表")
    @GetMapping("findAlbumAttributeValue/{albumId}")
    public Result<List<AlbumAttributeValue>> findAlbumAttributeValue(@PathVariable Long albumId)
    {
        //	获取到专辑属性集合
        List<AlbumAttributeValue> albumAttributeValueList = albumInfoService.findAlbumAttributeValueByAlbumId(albumId);
        return Result.ok(albumAttributeValueList);
    }

    @Operation(summary = "获取到专辑统计信息")
    @GetMapping("/getAlbumStatVo/{albumId}")
    public Result getAlbumStatVo(@PathVariable Long albumId)
    {
        //	获取服务层方法
        AlbumStatVo albumStatVo = this.albumInfoService.getAlbumStatVoByAlbumId(albumId);
        return Result.ok(albumStatVo);
    }


}

