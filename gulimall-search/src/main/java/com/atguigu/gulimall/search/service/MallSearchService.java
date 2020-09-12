package com.atguigu.gulimall.search.service;

import com.atguigu.gulimall.search.vo.SearchParm;
import com.atguigu.gulimall.search.vo.SearchResult;

public interface MallSearchService {

    /**
     *
     * @param searchParm 检索的所以参数
     * @return 返回检索的结果
     */
    SearchResult search(SearchParm searchParm);
}
