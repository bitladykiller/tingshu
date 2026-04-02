package com.atguigu.tingshu.search.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Tag(name = "搜索专辑管理")
@RestController
@RequestMapping("api/search/albumInfo")
@SuppressWarnings({"all"})
public class SearchApiController
{

    @Autowired
    private SearchService searchService;

    @Operation(summary = "上架专辑")
    @GetMapping("/upperAlbum/{albumId}")
    public Result upperAlbum(@PathVariable Long albumId)
    {
        this.searchService.upperAlbum(albumId);
        return Result.ok();
    }

    @Operation(summary = "下架专辑")
    @GetMapping("lowerAlbum/{albumId}")
    public Result lowerAlbum(@PathVariable Long albumId)
    {
        searchService.lowerAlbum(albumId);
        return Result.ok();
    }

    @Operation(summary = "批量上架")
    @GetMapping("batchUpperAlbum")
    public Result batchUpperAlbum()
    {
        //  循环
        for (long i = 1; i <= 1500; i++)
        {
            searchService.upperAlbum(i);
        }
        //  返回数据
        return Result.ok();
    }

    @Operation(summary = "专辑搜索列表")
    @PostMapping
    public Result search(@RequestBody AlbumIndexQuery albumIndexQuery) throws IOException
    {
        AlbumSearchResponseVo albumSearchResponseVo = searchService.search(albumIndexQuery);
        return Result.ok(albumSearchResponseVo);
    }

    @Operation(summary = "获取频道页数据")
    @GetMapping("channel/{category1Id}")
    public Result channel(@PathVariable Long category1Id)
    {

        List<Map<String, Object>> mapList = null;
        try
        {
            mapList = searchService.channel(category1Id);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        return Result.ok(mapList);
    }

    @Operation(summary = "关键字自动补全")
    @GetMapping("completeSuggest/{keyword}")
    public Result completeSuggest(@PathVariable String keyword)
    {
        List<String> list = searchService.completeSuggest(keyword);
        return Result.ok(list);
    }

    @SneakyThrows
    @Operation(summary = "更新排行榜")
    @GetMapping("updateLatelyAlbumRanking")
    public Result updateLatelyAlbumRanking()
    {
        searchService.updateLatelyAlbumRanking();
        return Result.ok();
    }

    @Operation(summary = "获取排行榜列表")
    @Parameters({
            @Parameter(name = "category1Id", description = "一级分类", in = ParameterIn.PATH, required = true),
            @Parameter(name = "dimension", description = "热度:hotScore、播放量:playStatNum、订阅量:subscribeStatNum、购买量:buyStatNum、评论数:albumCommentStatNum", required = true, in = ParameterIn.PATH),
    })
    @GetMapping("findRankingList/{category1Id}/{dimension}")
    public Result<List<AlbumInfoIndexVo>> findRankingList(@PathVariable Long category1Id,
                                                          @PathVariable String dimension)
    {
        List<AlbumInfoIndexVo> infoIndexVoList = searchService.findRankingList(category1Id,
                dimension);
        return Result.ok(infoIndexVoList);
    }
}

