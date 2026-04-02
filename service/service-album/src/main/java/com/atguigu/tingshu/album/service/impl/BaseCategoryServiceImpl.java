package com.atguigu.tingshu.album.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.*;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.model.album.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
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

    @Override
    public List<BaseCategory3> selectTopBaseCategory3(Long category1Id)
    {
        LambdaQueryWrapper<BaseCategory2> baseCategory2LambdaQueryWrapper = new LambdaQueryWrapper<>();
        baseCategory2LambdaQueryWrapper.eq(BaseCategory2::getCategory1Id,
                category1Id);
        List<BaseCategory2> baseCategory2List = baseCategory2Mapper.selectList(baseCategory2LambdaQueryWrapper);
        List<Long> category2IdList = baseCategory2List.stream().map(BaseCategory2::getId).collect(Collectors.toList());
        LambdaQueryWrapper<BaseCategory3> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(BaseCategory3::getCategory2Id,
                category2IdList).eq(BaseCategory3::getIsTop,
                1).last("limit 7");
        return baseCategory3Mapper.selectList(wrapper);
    }

    @Override
    public JSONObject getAllCategoryList(Long category1Id)
    {
        BaseCategory1 baseCategory1 = baseCategory1Mapper.selectById(category1Id);

        JSONObject category1 = new JSONObject();
        category1.put("categoryId",
                category1Id);
        category1.put("categoryName",
                baseCategory1 == null ? null : baseCategory1.getName());

        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(
                new LambdaQueryWrapper<BaseCategoryView>()
                        .eq(BaseCategoryView::getCategory1Id,
                                category1Id)
                        .orderByAsc(BaseCategoryView::getCategory2Id)
                        .orderByAsc(BaseCategoryView::getCategory3Id)
        );

        List<JSONObject> category2Child = new ArrayList<>();
        category1.put("categoryChild",
                category2Child);

        if (baseCategoryViewList == null || baseCategoryViewList.isEmpty())
        {
            return category1;
        }

        Long currentCategory2Id = null;
        JSONObject currentCategory2 = null;
        List<JSONObject> currentCategory3Child = null;

        for (BaseCategoryView view : baseCategoryViewList)
        {
            Long category2Id = view.getCategory2Id();

            if (!Objects.equals(currentCategory2Id,
                    category2Id))
            {
                currentCategory2Id = category2Id;

                currentCategory2 = new JSONObject();
                currentCategory2.put("categoryId",
                        category2Id);
                currentCategory2.put("categoryName",
                        view.getCategory2Name());

                currentCategory3Child = new ArrayList<>();
                currentCategory2.put("categoryChild",
                        currentCategory3Child);

                category2Child.add(currentCategory2);
            }

            if (view.getCategory3Id() != null)
            {
                JSONObject category3 = new JSONObject();
                category3.put("categoryId",
                        view.getCategory3Id());
                category3.put("categoryName",
                        view.getCategory3Name());
                currentCategory3Child.add(category3);
            }
        }

        return category1;
    }

    @Override
    public List<BaseCategory1> findAllCategory1()
    {
        return baseCategory1Mapper.selectList(null);
    }
}
