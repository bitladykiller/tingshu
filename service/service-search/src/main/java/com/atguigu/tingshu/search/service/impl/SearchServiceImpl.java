package com.atguigu.tingshu.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.CategoryFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.repository.AlbumInfoIndexRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.impl.UserInfoDegradeFeignClient;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


@Slf4j
@Service
@SuppressWarnings({"all"})
public class SearchServiceImpl implements SearchService
{


    @Autowired
    private AlbumInfoIndexRepository albumInfoIndexRepository;

    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Autowired
    private CategoryFeignClient categoryFeignClient;
    @Autowired
    private UserInfoDegradeFeignClient userInfoDegradeFeignClient;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
    @Autowired
    private ElasticsearchClient elasticsearchClient;


    @Override
    public void upperAlbum(Long albumId)
    {


        CompletableFuture<AlbumInfo> albumFuture = CompletableFuture.supplyAsync(() ->
        {
            Result<AlbumInfo> result = albumInfoFeignClient.getAlbumInfo(albumId);
            Assert.notNull(result, "专辑返回结果为空");
            Assert.notNull(result.getData(), "专辑为空");
            return result.getData();
        }, threadPoolExecutor);

        CompletableFuture<List<AttributeValueIndex>> attrFuture = CompletableFuture.supplyAsync(() ->
        {
            Result<List<AlbumAttributeValue>> result = albumInfoFeignClient.findAlbumAttributeValue(albumId);
            Assert.notNull(result, "专辑属性结果集为空");
            Assert.notNull(result.getData(), "专辑属性为空");

            return result.getData().stream().map(item ->
            {
                AttributeValueIndex index = new AttributeValueIndex();

                BeanUtils.copyProperties(item, index);
                return index;
            }).collect(Collectors.toList());
        }, threadPoolExecutor);

        CompletableFuture<BaseCategoryView> categoryFuture = albumFuture.thenApplyAsync(albumInfo ->
        {
            Result<BaseCategoryView> result = categoryFeignClient.getCategoryView(albumInfo.getCategory3Id());
            Assert.notNull(result, "专辑分类结果集为空");
            Assert.notNull(result.getData(), "专辑分类为空");
            return result.getData();
        }, threadPoolExecutor);

        CompletableFuture<UserInfoVo> userFuture = albumFuture.thenApplyAsync(albumInfo ->
        {
            Result<UserInfoVo> result = userInfoDegradeFeignClient.getUserInfoVo(albumInfo.getUserId());
            Assert.notNull(result, "用户结果集为空");
            Assert.notNull(result.getData(), "用户为空");
            return result.getData();
        }, threadPoolExecutor);

        CompletableFuture.allOf(albumFuture, attrFuture, categoryFuture, userFuture).join();

        AlbumInfo albumInfo = albumFuture.join();
        List<AttributeValueIndex> attrList = attrFuture.join();
        BaseCategoryView categoryView = categoryFuture.join();
        UserInfoVo userInfoVo = userFuture.join();

        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();

        BeanUtils.copyProperties(albumInfo, albumInfoIndex);

        albumInfoIndex.setAttributeValueIndexList(attrList);

        albumInfoIndex.setCategory1Id(categoryView.getCategory1Id());
        albumInfoIndex.setCategory2Id(categoryView.getCategory2Id());
        albumInfoIndex.setCategory3Id(categoryView.getCategory3Id());

        albumInfoIndex.setAnnouncerName(userInfoVo.getNickname());

        albumInfoIndex.setPlayStatNum(ThreadLocalRandom.current().nextInt(100000));
        albumInfoIndex.setSubscribeStatNum(ThreadLocalRandom.current().nextInt(100000000));
        albumInfoIndex.setBuyStatNum(ThreadLocalRandom.current().nextInt(10000000));
        albumInfoIndex.setCommentStatNum(ThreadLocalRandom.current().nextInt(1000000000));

        albumInfoIndexRepository.save(albumInfoIndex);
    }

    @Override
    public void lowerAlbum(Long albumId)
    {
        albumInfoIndexRepository.deleteById(albumId);
    }

    @Override
    public AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery)
    {
        // 1. 构建 Elasticsearch 查询请求
        SearchRequest request = buildQueryDsl(albumIndexQuery);

        SearchResponse<AlbumInfoIndex> response;
        try
        {
            // 2. 执行查询
            response = elasticsearchClient.search(request, AlbumInfoIndex.class);
        } catch (IOException e)
        {
            // 3. 实际项目中建议打印日志，并包装成业务异常
            throw new RuntimeException("调用 Elasticsearch 查询专辑索引失败", e);
        }

        // 4. 解析查询结果
        AlbumSearchResponseVo responseVo = parseSearchResult(response);

        // 5. 回填分页信息
        Integer pageNo = albumIndexQuery.getPageNo();
        Integer pageSize = albumIndexQuery.getPageSize();
        responseVo.setPageNo(pageNo);
        responseVo.setPageSize(pageSize);

        // 6. 计算总页数，使用向上取整公式
        long total = responseVo.getTotal() == null ? 0L : responseVo.getTotal();
        long totalPages = (total + pageSize - 1) / pageSize;
        responseVo.setTotalPages(totalPages);

        return responseVo;
    }

    private SearchRequest buildQueryDsl(AlbumIndexQuery albumIndexQuery)
    {
        SearchRequest.Builder requestBuilder = new SearchRequest.Builder();
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        // =========================
        // 1. 关键词检索
        // =========================
        String keyword = albumIndexQuery.getKeyword();
        if (!StringUtils.isEmpty(keyword))
        {
            // 这里使用一个内部 bool，将标题和简介做 should 匹配，
            // 然后通过 minimumShouldMatch("1") 保证至少命中一个字段
            boolQuery.must(q -> q.bool(b -> b.should(s -> s.match(m -> m.field("albumTitle").query(keyword)))
                    .should(s -> s.match(m -> m.field("albumIntro").query(keyword))).minimumShouldMatch("1")));

            // 高亮标题字段
            requestBuilder.highlight(
                    h -> h.fields("albumTitle", f -> f.preTags("<span style='color:red'>").postTags("</span>")));
        }

        // =========================
        // 2. 分类过滤
        // =========================
        Long category1Id = albumIndexQuery.getCategory1Id();
        if (category1Id != null)
        {
            boolQuery.filter(f -> f.term(t -> t.field("category1Id").value(category1Id)));
        }

        Long category2Id = albumIndexQuery.getCategory2Id();
        if (category2Id != null)
        {
            boolQuery.filter(f -> f.term(t -> t.field("category2Id").value(category2Id)));
        }

        Long category3Id = albumIndexQuery.getCategory3Id();
        if (category3Id != null)
        {
            boolQuery.filter(f -> f.term(t -> t.field("category3Id").value(category3Id)));
        }

        // =========================
        // 3. 属性过滤（nested）
        // =========================
        List<String> attributeList = albumIndexQuery.getAttributeList();
        if (!CollectionUtils.isEmpty(attributeList))
        {
            for (String attribute : attributeList)
            {
                if (StringUtils.isEmpty(attribute) || !attribute.contains(":"))
                {
                    continue;
                }

                String[] split = attribute.split(":");
                if (split.length != 2)
                {
                    continue;
                }

                String attributeId = split[0];
                String valueId = split[1];

                // 注意：
                // attributeValueIndexList 是 nested 类型字段时，
                // 必须使用 nested query，避免不同数组元素之间错位匹配。
                boolQuery.filter(f -> f.nested(n -> n.path("attributeValueIndexList").query(q -> q.bool(
                        b -> b.must(m -> m.term(t -> t.field("attributeValueIndexList.attributeId").value(attributeId)))
                                .must(m -> m.term(t -> t.field("attributeValueIndexList.valueId").value(valueId)))))));
            }
        }

        // =========================
        // 4. 排序
        // =========================
        String order = albumIndexQuery.getOrder();
        if (!StringUtils.isEmpty(order) && order.contains(":"))
        {
            String[] split = order.split(":");
            if (split.length == 2)
            {
                String sortType = split[0];
                String sortDirection = split[1];

                String orderField = null;
                switch (sortType)
                {
                    case "1":
                        orderField = "hotScore";
                        break;
                    case "2":
                        orderField = "playStatNum";
                        break;
                    case "3":
                        orderField = "createTime";
                        break;
                    default:
                        break;
                }

                // 只有字段合法时才排序
                if (!StringUtils.isEmpty(orderField))
                {
                    SortOrder sortOrder = "asc".equalsIgnoreCase(sortDirection) ? SortOrder.Asc : SortOrder.Desc;

                    String finalOrderField = orderField;
                    requestBuilder.sort(s -> s.field(f -> f.field(finalOrderField).order(sortOrder)));
                }
            }
        } else
        {
            // 默认按相关性评分倒序
            requestBuilder.sort(s -> s.field(f -> f.field("_score").order(SortOrder.Desc)));
        }

        // =========================
        // 5. 返回字段控制
        // =========================
        requestBuilder.source(s -> s.filter(f -> f.excludes("attributeValueIndexList")));

        // =========================
        // 6. 分页
        // =========================
        int pageNo = albumIndexQuery.getPageNo() == null || albumIndexQuery.getPageNo() < 1 ? 1 :
                albumIndexQuery.getPageNo();
        int pageSize = albumIndexQuery.getPageSize() == null || albumIndexQuery.getPageSize() < 1 ? 10 :
                albumIndexQuery.getPageSize();

        int from = (pageNo - 1) * pageSize;
        requestBuilder.from(from);
        requestBuilder.size(pageSize);

        // =========================
        // 7. 指定索引和查询条件
        // =========================
        requestBuilder.index("albuminfo").query(q -> q.bool(boolQuery.build()));

        SearchRequest searchRequest = requestBuilder.build();
        System.out.println("dsl = " + searchRequest);
        return searchRequest;
    }

    private AlbumSearchResponseVo parseSearchResult(SearchResponse<AlbumInfoIndex> searchResponse)
    {
        AlbumSearchResponseVo responseVo = new AlbumSearchResponseVo();

        HitsMetadata<AlbumInfoIndex> hits = searchResponse.hits();

        // 设置总条数
        if (hits.total() != null)
        {
            responseVo.setTotal(hits.total().value());
        } else
        {
            responseVo.setTotal(0L);
        }

        List<Hit<AlbumInfoIndex>> hitList = hits.hits();
        if (CollectionUtils.isEmpty(hitList))
        {
            responseVo.setList(new ArrayList<>());
            return responseVo;
        }

        List<AlbumInfoIndexVo> list = hitList.stream().map(hit ->
        {
            AlbumInfoIndexVo vo = new AlbumInfoIndexVo();
            AlbumInfoIndex source = hit.source();

            if (source != null)
            {
                BeanUtils.copyProperties(source, vo);
            }

            // 高亮处理要注意空指针保护
            if (hit.highlight() != null && hit.highlight().get("albumTitle") != null &&
                    !hit.highlight().get("albumTitle").isEmpty())
            {
                vo.setAlbumTitle(hit.highlight().get("albumTitle").get(0));
            }

            return vo;
        }).collect(Collectors.toList());

        responseVo.setList(list);
        return responseVo;
    }
}
