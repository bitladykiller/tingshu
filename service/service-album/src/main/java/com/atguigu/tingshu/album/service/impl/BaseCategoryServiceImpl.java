package com.atguigu.tingshu.album.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.*;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"all"})
public class BaseCategoryServiceImpl extends ServiceImpl<BaseCategory1Mapper, BaseCategory1> implements BaseCategoryService
{

    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;


    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Autowired
    private BaseAttributeMapper baseAttributeMapper;

    @Override
    public List<JSONObject> getBaseCategoryList()
    {
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);
        ArrayList<JSONObject> finalList = new ArrayList<>();
        Map<Long, List<BaseCategoryView>> listMap = baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        listMap.forEach((k, v) ->
        {
            Long key = k;
            List<BaseCategoryView> list1 = v;
            JSONObject jsonObject1 = new JSONObject();
            jsonObject1.put("categoryId",
                    key);
            jsonObject1.put("categoryName",
                    list1.get(0).getCategory1Name());
            List<JSONObject> categoryList2 = new ArrayList<>();
            Map<Long, List<BaseCategoryView>> listMap2 = list1.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            listMap2.forEach((k2, v2) ->
            {
                Long key2 = k2;
                List<BaseCategoryView> list2 = v2;
                JSONObject jsonObject2 = new JSONObject();
                List<JSONObject> categoryList3 = list2.stream().map(baseCategoryView ->
                {
                    JSONObject jsonObject3 = new JSONObject();
                    jsonObject3.put("categoryId",
                            baseCategoryView.getCategory3Id());
                    jsonObject3.put("categoryName",
                            baseCategoryView.getCategory3Name());
                    return jsonObject3;
                }).collect(Collectors.toList());
                jsonObject2.put("categoryId",
                        key2);
                jsonObject2.put("categoryName",
                        list2.get(0).getCategory2Name());
                jsonObject2.put("categoryChild",
                        categoryList3);
                categoryList2.add(jsonObject2);
            });
            jsonObject1.put("categoryChild",
                    categoryList2);
            finalList.add(jsonObject1);
        });
        return finalList;
    }

    @Override
    public List<BaseAttribute> findAttribute(Long category1Id)
    {
        return baseAttributeMapper.selectAttribute(category1Id);
    }

    @Override
    public BaseCategoryView getCategoryView(Long category3Id)
    {
        LambdaQueryWrapper<BaseCategoryView> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BaseCategoryView::getCategory3Id,
                category3Id);
        BaseCategoryView baseCategoryView = baseCategoryViewMapper.selectOne(queryWrapper);
        return baseCategoryView;
    }
}
