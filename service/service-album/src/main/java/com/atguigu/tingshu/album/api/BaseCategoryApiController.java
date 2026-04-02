package com.atguigu.tingshu.album.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "分类管理")
@RestController
@RequestMapping(value = "/api/album/category")
@SuppressWarnings({"all"})
public class BaseCategoryApiController
{

    @Autowired
    private BaseCategoryService baseCategoryService;

    @GetMapping("getBaseCategoryLits")
    public Result getBaseCategoryLits()
    {
        List<JSONObject> list = baseCategoryService.getBaseCategoryList();
        return Result.ok(list);
    }

    @GetMapping("/findAttribute/{category1Id}")
    @Operation(summary = "根据一级分类id查询属性")
    public Result findAttribute(@PathVariable("category1Id") Long category1Id)
    {
        List<BaseAttribute> list = baseCategoryService.findAttribute(category1Id);
        return Result.ok(list);
    }

    @Operation(summary = "根据三级分类的名称查询到一级分类和二级分裂")
    @GetMapping("getCategoryView/{category3Id}")
    public Result<BaseCategoryView> getCategoryView(@PathVariable("category3Id") Long category3Id)
    {
        BaseCategoryView baseCategoryView = baseCategoryService.getCategoryView(category3Id);
        return Result.ok(baseCategoryView);
    }

    @Operation(summary = "获取一级分类下的所有三级分类的对象")
    @GetMapping("findToBaseCategory3/{category1Id}")
    public Result<List<BaseCategory3>> findToBaseCategory3(@PathVariable("category1Id") Long category1Id)
    {
        List<BaseCategory3> baseCategory3List = baseCategoryService.selectTopBaseCategory3(category1Id);
        return Result.ok(baseCategory3List);
    }

    @Operation(summary = "根据一级分类id获取全部分类信息")
    @GetMapping("getBaseCategoryList/{category1Id}")
    public Result<JSONObject> getBaseCategoryList(@PathVariable Long category1Id)
    {
        JSONObject jsonObject = baseCategoryService.getAllCategoryList(category1Id);
        return Result.ok(jsonObject);
    }

    @Operation(summary = "查询所有的一级分类信息")
    @GetMapping("findAllCategory1")
    public Result<List<BaseCategory1>> findAllCategory1()
    {
        List<BaseCategory1> baseCategory1List = baseCategoryService.findAllCategory1();
        return Result.ok(baseCategory1List);
    }


}

