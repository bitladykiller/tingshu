package com.atguigu.tingshu.album.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@Tag(name = "分类管理")
@RestController
@RequestMapping(value="/api/album/category")
@SuppressWarnings({"all"})
public class BaseCategoryApiController {
	
	@Autowired
	private BaseCategoryService baseCategoryService;

	@GetMapping("getBaseCategoryLits")
	public Result getBaseCategoryLits() {
		List<JSONObject> list = baseCategoryService.getBaseCategoryList();
		return Result.ok(list);
	}

	@GetMapping("/findAttribute/{category1Id}")
	@Operation(summary = "根据一级分类id查询属性")
	public Result findAttribute(@PathVariable("category1Id") Long category1Id) {
		List<BaseAttribute> list =baseCategoryService.findAttribute(category1Id);
		return Result.ok(list);
	}

	@Operation(summary = "根据三级分类的名称查询到一级分类和二级分裂")
	@GetMapping("getCategoryView/{category3Id}")
	public Result<BaseCategoryView> getCategoryView(@PathVariable("category3Id") Long category3Id)
	{
		BaseCategoryView baseCategoryView = baseCategoryService.getCategoryView(category3Id);
		return Result.ok(baseCategoryView);
	}


}

