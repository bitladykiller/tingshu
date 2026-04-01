package com.atguigu.tingshu.search.service;

import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface SearchService {


    void upperAlbum(Long albumId);

    void lowerAlbum(Long albumId);

    AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery);

    List<Map<String, Object>> channel(Long category1Id) throws IOException;

    List<String> completeSuggest(String keyword);
}
