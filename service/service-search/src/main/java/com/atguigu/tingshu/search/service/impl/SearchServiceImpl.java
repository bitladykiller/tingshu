package com.atguigu.tingshu.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.CategoryFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.PinYinUtils;
import com.atguigu.tingshu.model.album.*;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.model.search.SuggestIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.repository.AlbumInfoIndexRepository;
import com.atguigu.tingshu.search.repository.SuggestIndexRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
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
    private UserInfoFeignClient userInfoFeignClient;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
    @Autowired
    private ElasticsearchClient elasticsearchClient;
    @Autowired
    private SuggestIndexRepository suggestIndexRepository;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;


    @Override
    public void upperAlbum(Long albumId)
    {


        CompletableFuture<AlbumInfo> albumFuture = CompletableFuture.supplyAsync(() ->
                {
                    Result<AlbumInfo> result = albumInfoFeignClient.getAlbumInfo(albumId);
                    Assert.notNull(result,
                            "专辑返回结果为空");
                    Assert.notNull(result.getData(),
                            "专辑为空");
                    return result.getData();
                },
                threadPoolExecutor);

        CompletableFuture<List<AttributeValueIndex>> attrFuture = CompletableFuture.supplyAsync(() ->
                {
                    Result<List<AlbumAttributeValue>> result = albumInfoFeignClient.findAlbumAttributeValue(albumId);
                    Assert.notNull(result,
                            "专辑属性结果集为空");
                    Assert.notNull(result.getData(),
                            "专辑属性为空");

                    return result.getData().stream().map(item ->
                    {
                        AttributeValueIndex index = new AttributeValueIndex();

                        BeanUtils.copyProperties(item,
                                index);
                        return index;
                    }).collect(Collectors.toList());
                },
                threadPoolExecutor);

        CompletableFuture<BaseCategoryView> categoryFuture = albumFuture.thenApplyAsync(albumInfo ->
                {
                    Result<BaseCategoryView> result = categoryFeignClient.getCategoryView(albumInfo.getCategory3Id());
                    Assert.notNull(result,
                            "专辑分类结果集为空");
                    Assert.notNull(result.getData(),
                            "专辑分类为空");
                    return result.getData();
                },
                threadPoolExecutor);

        CompletableFuture<UserInfoVo> userFuture = albumFuture.thenApplyAsync(albumInfo ->
                {
                    Result<UserInfoVo> result = userInfoFeignClient.getUserInfoVo(albumInfo.getUserId());
                    Assert.notNull(result,
                            "用户结果集为空");
                    Assert.notNull(result.getData(),
                            "用户为空");
                    return result.getData();
                },
                threadPoolExecutor);

        CompletableFuture.allOf(albumFuture,
                attrFuture,
                categoryFuture,
                userFuture).join();

        AlbumInfo albumInfo = albumFuture.join();
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        bloomFilter.add(albumInfo.getId());
        List<AttributeValueIndex> attrList = attrFuture.join();
        BaseCategoryView categoryView = categoryFuture.join();
        UserInfoVo userInfoVo = userFuture.join();

        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();

        BeanUtils.copyProperties(albumInfo,
                albumInfoIndex);

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

        SuggestIndex suggestIndex = new SuggestIndex();
        suggestIndex.setId(UUID.randomUUID().toString().replaceAll("-",
                ""));
        suggestIndex.setTitle(albumInfoIndex.getAlbumTitle());
        suggestIndex.setKeyword(new Completion(new String[]{albumInfoIndex.getAlbumTitle()}));
        suggestIndex.setKeywordPinyin(new Completion(new String[]{PinYinUtils.toHanyuPinyin(albumInfoIndex.getAlbumTitle())}));
        suggestIndex.setKeywordSequence(new Completion(new String[]{PinYinUtils.getFirstLetter(albumInfoIndex.getAlbumTitle())}));
        suggestIndexRepository.save(suggestIndex);
    }

    @Override
    public void lowerAlbum(Long albumId)
    {
        albumInfoIndexRepository.deleteById(albumId);
    }

    @Override
    public AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery)
    {
        SearchRequest request = buildQueryDsl(albumIndexQuery);

        SearchResponse<AlbumInfoIndex> response;
        try
        {
            response = elasticsearchClient.search(request,
                    AlbumInfoIndex.class);
        } catch (IOException e)
        {
            throw new RuntimeException("调用 Elasticsearch 查询专辑索引失败",
                    e);
        }

        AlbumSearchResponseVo responseVo = parseSearchResult(response);

        Integer pageNo = albumIndexQuery.getPageNo();
        Integer pageSize = albumIndexQuery.getPageSize();
        responseVo.setPageNo(pageNo);
        responseVo.setPageSize(pageSize);

        long total = responseVo.getTotal() == null ? 0L : responseVo.getTotal();
        long totalPages = (total + pageSize - 1) / pageSize;
        responseVo.setTotalPages(totalPages);

        return responseVo;
    }

    @Override
    public List<Map<String, Object>> channel(Long category1Id)
    {
        Result<List<BaseCategory3>> baseCategory3ListResult = categoryFeignClient.findTopBaseCategory3(category1Id);
        List<BaseCategory3> baseCategory3List = baseCategory3ListResult.getData();
        Map<Long, BaseCategory3> category3IdToMap = baseCategory3List.stream().collect(Collectors.toMap(BaseCategory3::getId,
                baseCategory3 -> baseCategory3));
        List<Long> idList = baseCategory3List.stream().map(BaseCategory3::getId).collect(Collectors.toList());
        List<FieldValue> valueList = idList.stream().map(id -> FieldValue.of(id)).collect(Collectors.toList());
        SearchRequest.Builder request = new SearchRequest.Builder();
        request.index("albuminfo").query(q -> q.terms(f -> f.field("category3Id").terms(new TermsQueryField.Builder().value(valueList).build())));
        request.aggregations("groupByCategory3IdAgg",
                a -> a.terms(t -> t.field("category3Id"))
                        .aggregations("topTenHotScoreAgg",
                                a1 -> a1.topHits(s -> s.size(6).sort(sort -> sort.field(f -> f.field("hotScore").order(SortOrder.Desc))))));
        SearchResponse<AlbumInfoIndex> searchResponse = null;
        try
        {
            searchResponse = elasticsearchClient.search(request.build(),
                    AlbumInfoIndex.class);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        Aggregate groupByCategory3IdAgg = searchResponse.aggregations().get("groupByCategory3IdAgg");
        groupByCategory3IdAgg.lterms().buckets().array().forEach(item ->
        {
            List<AlbumInfoIndex> albumInfoIndexList = new ArrayList<>();
            long category3Id = item.key();
            Aggregate topTenHotScoreAgg = item.aggregations().get("topTenHotScoreAgg");
            topTenHotScoreAgg.topHits().hits().hits().forEach(hit ->
            {
                String json = hit.source().toString();
                AlbumInfoIndex albumInfoIndex = JSON.parseObject(json,
                        AlbumInfoIndex.class);
                albumInfoIndexList.add(albumInfoIndex);
            });
            Map<String, Object> map = new HashMap<>();
            map.put("baseCategory3",
                    category3IdToMap.get(category3Id));
            map.put("list",
                    albumInfoIndexList);
            result.add(map);
        });
        return result;
    }

    @Override
    public List<String> completeSuggest(String keyword)
    {
        SearchRequest.Builder searchRequest = new SearchRequest.Builder();
        searchRequest.index("suggestinfo").suggest(
                s -> s.suggesters("suggestionKeyword",
                                f -> f.prefix(keyword).completion(
                                        c -> c.field("keyword").skipDuplicates(true).size(10)
                                                .fuzzy(
                                                        z -> z.fuzziness("auto"))
                                ))
                        .suggesters("suggestionkeywordPinyin",
                                f -> f.prefix(keyword).completion(
                                        c -> c.field("keywordPinyin").skipDuplicates(true).size(10)
                                                .fuzzy(z -> z.fuzziness("auto"))
                                ))
                        .suggesters("suggestionkeywordSequence",
                                f -> f.prefix(keyword).completion(
                                        c -> c.field("keywordSequence").skipDuplicates(true).size(10)
                                                .fuzzy(z -> z.fuzziness("auto"))
                                ))
        );
        SearchResponse<SuggestIndex> searchResponse = null;
        try
        {
            searchResponse = elasticsearchClient.search(searchRequest.build(),
                    SuggestIndex.class);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        HashSet<String> titleSet = new HashSet<>();
        titleSet.addAll(this.parseResultData(searchResponse,
                "suggestionKeyword"));
        titleSet.addAll(this.parseResultData(searchResponse,
                "suggestionkeywordPinyin"));
        titleSet.addAll(this.parseResultData(searchResponse,
                "suggestionkeywordSequence"));

        if (titleSet.size() < 10)
        {
            SearchResponse<SuggestIndex> response = null;
            try
            {
                response = elasticsearchClient.search(s -> s.index("suggestinfo")
                                .query(f -> f.match(m -> m.field("title").query(keyword)))
                        ,
                        SuggestIndex.class);
            } catch (IOException e)
            {
                throw new RuntimeException(e);
            }
            for (Hit<SuggestIndex> hit : response.hits().hits())
            {
                SuggestIndex suggestIndex = hit.source();
                titleSet.add(suggestIndex.getTitle());
                if (titleSet.size() == 10)
                {
                    break;
                }
            }
        }
        return new ArrayList<>(titleSet);
    }

    @Override
    public void updateLatelyAlbumRanking()
    {
        Result<List<BaseCategory1>> baseCategory1Result = categoryFeignClient.findAllCategory1();
        Assert.notNull(baseCategory1Result,
                "一级分类结果集为空");
        List<BaseCategory1> baseCategory1List = baseCategory1Result.getData();
        Assert.notNull(baseCategory1List,
                "一级分类集合为空");
        for (BaseCategory1 baseCategory1 : baseCategory1List)
        {
            String[] rankingDimensionArray = new String[]{"hotScore", "playStatNum", "subscribeStatNum", "buyStatNum", "commentStatNum"};
            for (String ranging : rankingDimensionArray)
            {
                SearchResponse<AlbumInfoIndex> response = null;
                try
                {
                    response = elasticsearchClient.search(f -> f.index("albuminfo")
                                    .query(q -> q.term(t -> t.field("category1Id").value(baseCategory1.getId())))
                                    .sort(s -> s.field(d -> d.field(ranging).order(SortOrder.Desc)))
                                    .size(10),
                            AlbumInfoIndex.class);
                } catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
                List<AlbumInfoIndex> albumInfoIndexList = response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
                String rangKey = RedisConstant.RANKING_KEY_PREFIX + baseCategory1.getId();
                redisTemplate.boundHashOps(rangKey).put(ranging,
                        albumInfoIndexList);
            }
        }
    }

    @Override
    public List<AlbumInfoIndexVo> findRankingList(Long category1Id,
                                                  String dimension)
    {
        return (List<AlbumInfoIndexVo>) redisTemplate.boundHashOps(RedisConstant.RANKING_KEY_PREFIX + category1Id).get(dimension);
    }

    private List<String> parseResultData(SearchResponse<SuggestIndex> response,
                                         String suggestName)
    {
        List<String> suggestList = new ArrayList<>();
        Map<String, List<Suggestion<SuggestIndex>>> groupBySuggestionListAggMap = response.suggest();
        groupBySuggestionListAggMap.get(suggestName).forEach(item ->
        {
            CompletionSuggest<SuggestIndex> completionSuggest = item.completion();
            completionSuggest.options().forEach(it ->
            {
                SuggestIndex suggestIndex = it.source();
                suggestList.add(suggestIndex.getTitle());
            });
        });
        return suggestList;
    }

    private SearchRequest buildQueryDsl(AlbumIndexQuery albumIndexQuery)
    {
        SearchRequest.Builder requestBuilder = new SearchRequest.Builder();
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        String keyword = albumIndexQuery.getKeyword();
        if (!StringUtils.isEmpty(keyword))
        {
            boolQuery.must(q -> q.bool(b -> b.should(s -> s.match(m -> m.field("albumTitle").query(keyword)))
                    .should(s -> s.match(m -> m.field("albumIntro").query(keyword))).minimumShouldMatch("1")));

            requestBuilder.highlight(
                    h -> h.fields("albumTitle",
                            f -> f.preTags("<span style='color:red'>").postTags("</span>")));
        }
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
                boolQuery.filter(f -> f.nested(n -> n.path("attributeValueIndexList").query(q -> q.bool(
                        b -> b.must(m -> m.term(t -> t.field("attributeValueIndexList.attributeId").value(attributeId)))
                                .must(m -> m.term(t -> t.field("attributeValueIndexList.valueId").value(valueId)))))));
            }
        }
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
                if (!StringUtils.isEmpty(orderField))
                {
                    SortOrder sortOrder = "asc".equalsIgnoreCase(sortDirection) ? SortOrder.Asc : SortOrder.Desc;

                    String finalOrderField = orderField;
                    requestBuilder.sort(s -> s.field(f -> f.field(finalOrderField).order(sortOrder)));
                }
            }
        } else
        {
            requestBuilder.sort(s -> s.field(f -> f.field("_score").order(SortOrder.Desc)));
        }
        requestBuilder.source(s -> s.filter(f -> f.excludes("attributeValueIndexList")));
        int pageNo = albumIndexQuery.getPageNo() == null || albumIndexQuery.getPageNo() < 1 ? 1 :
                albumIndexQuery.getPageNo();
        int pageSize = albumIndexQuery.getPageSize() == null || albumIndexQuery.getPageSize() < 1 ? 10 :
                albumIndexQuery.getPageSize();

        int from = (pageNo - 1) * pageSize;
        requestBuilder.from(from);
        requestBuilder.size(pageSize);
        requestBuilder.index("albuminfo").query(q -> q.bool(boolQuery.build()));

        SearchRequest searchRequest = requestBuilder.build();
        System.out.println("dsl = " + searchRequest);
        return searchRequest;
    }

    private AlbumSearchResponseVo parseSearchResult(SearchResponse<AlbumInfoIndex> searchResponse)
    {
        AlbumSearchResponseVo responseVo = new AlbumSearchResponseVo();

        HitsMetadata<AlbumInfoIndex> hits = searchResponse.hits();

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
                BeanUtils.copyProperties(source,
                        vo);
            }

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
