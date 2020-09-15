package com.atguigu.gulimall.search.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.gulimall.search.config.GulimallElasticSearchConfig;
import com.atguigu.gulimall.search.constant.EsConstant;
import com.atguigu.gulimall.search.service.MallSearchService;
import com.atguigu.gulimall.search.vo.SearchParm;
import com.atguigu.gulimall.search.vo.SearchResult;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MallSearchServiceImpl implements MallSearchService {

    @Resource
    private RestHighLevelClient client;

    //去es进行检索
    @Override
    public SearchResult search(SearchParm parm) {
        //1.动态构建出查询需要的dsl语句
        SearchResult result = null;
        //1.准备检索请求
        SearchRequest searchRequest = buildSearchRequrest(parm);

        try {
            //2.执行检索请求
            SearchResponse response = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
            //3.分析响应数据封装成我们想要的格式
            result = buildSearchResult(response, parm);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 准备检索请求
     * 模糊查询 过滤 （按照属性，分类，品牌，价格区间，库存），排序，分页，高亮，聚合分析
     *
     * @return
     */
    private SearchRequest buildSearchRequrest(SearchParm parm) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();//构造dsl语句的
        /**
         * 模糊查询 过滤 （按照属性，分类，品牌，价格区间，库存）
         */
        //1.构建bool-query
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //1.1 must 模糊匹配
        if (StrUtil.isNotEmpty(parm.getKeyWord())) {
            boolQuery.must(QueryBuilders.matchQuery("skuTitle", parm.getKeyWord()));
        }
        //1.2 bool-filter 按照三级分类id查询
        if (parm.getCatelog3Id() != null) {
            boolQuery.filter(QueryBuilders.termQuery("catelogId", parm.getCatelog3Id()));
        }
        //1.3 bool-filter 按照品牌id进行查询
        if (parm.getBrandId() != null) {
            boolQuery.filter(QueryBuilders.termsQuery("brandId", parm.getBrandId()));
        }
        //1.4 bool-filter 按照所有属性进行查询

        if (parm.getAttrs() != null && parm.getAttrs().size() > 0) {

            for (String attrStr : parm.getAttrs()) {
                // &attrs=1_5寸：8寸&attrs=2_16G:8G
                BoolQueryBuilder nestBoolQuery = QueryBuilders.boolQuery();
                //attr=1_5寸：8寸
                String[] s = attrStr.split("_");
                String attrId = s[0];//检索属性id
                String[] attrValues = s[1].split(":");//这个属性的检索用的值
                nestBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                nestBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                //关键点 每一个必须都得生成一个nested查询，注意fiter添加的是nested的对象
                NestedQueryBuilder nestedQueryBuilder = QueryBuilders.nestedQuery("attrs", nestBoolQuery, ScoreMode.None);
                boolQuery.filter(nestedQueryBuilder);
            }
        }

        //1.5 bool-filter 按照是否有库存进行查询 是否等于1 true false判断
        if (parm.getHasStock() != null) {
            boolQuery.filter(QueryBuilders.termQuery("hasStock", parm.getHasStock() == 1));
        }

        //1.6  bool-filter 按照价格区间进行查询
        if (StrUtil.isNotEmpty(parm.getSkuPrice())) {
            //skuPrice=1_50/_500/500_
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] s = parm.getSkuPrice().split("_");
            if (s.length == 2) {
                rangeQuery.gte(s[0]).lte(s[1]);
            } else if (s.length == 1) {
                if (parm.getSkuPrice().startsWith("_")) {
                    rangeQuery.lte(s[0]);
                }
                if (parm.getSkuPrice().endsWith("_")) {
                    rangeQuery.gte(s[0]);
                }
            }
            boolQuery.filter(rangeQuery);
        }

        //把以上的所有条件都拿来进行封装
        sourceBuilder.query(boolQuery);

        /**
         * 排序，分页，高亮
         */
        //2.1 排序
        if (StrUtil.isNotEmpty(parm.getSort())) {
            String sort = parm.getSort();
            //sort=saleCount_asc
            String[] s = sort.split("_");
            SortOrder order = s[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
            sourceBuilder.sort(s[0], order);
        }
        //2.2 分页 pagesize:5
        // pageNum:1 from:0 size[5] [0,1,2,3,4]
        sourceBuilder.from((parm.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE);
        sourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);

        //2.3 高亮
        if (StrUtil.isNotBlank(parm.getKeyWord())) {
            HighlightBuilder builder = new HighlightBuilder();
            builder.field("skuTitle");
            builder.preTags("<b style='color:red'>");
            builder.postTags("</b>");
            sourceBuilder.highlighter(builder);
        }
        /**
         * 聚合分析
         */
        //1.品牌聚合
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
        //1.1要聚合的品牌id和数量
        brand_agg.field("brandId").size(20);
        //1.2品牌聚合的子聚合
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        //todo 聚合brand
        sourceBuilder.aggregation(brand_agg);

        //2.分类聚合 catelog_agg
        TermsAggregationBuilder catelog_agg = AggregationBuilders.terms("catelog_agg").field("catelogId").size(20);
        //2.1 分类名称子聚合
        catelog_agg.subAggregation(AggregationBuilders.terms("catelog_name_agg").field("catelogName").size(1));
        //todo 聚合catelog
        sourceBuilder.aggregation(catelog_agg);

        //3.属性聚合 attr_agg
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        //聚合出当前所有的attrId
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        //聚合出当前attr_id对应的名字
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        //聚合分析当前attr_id对应的所有可能的属性值 attrValue
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        attr_agg.subAggregation(attr_id_agg);
        //todo 聚合attr
        sourceBuilder.aggregation(attr_agg);

        String s = sourceBuilder.toString();
        System.out.println("构建的DSL" + s);

        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, sourceBuilder);
        return searchRequest;
    }

    /**
     * 构造结果数据
     *
     * @param response
     * @return
     */
    private SearchResult buildSearchResult(SearchResponse response, SearchParm parm) {

        SearchResult result = new SearchResult();
        //1、返回的所有查询到的商品
        SearchHits hits = response.getHits();
        ArrayList<SkuEsModel> esModels = new ArrayList<>();
        if (hits.getHits() != null && hits.getHits().length > 0)
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel esModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                //如果是全局搜索带上高亮
                if (StrUtil.isNotBlank(parm.getKeyWord())){
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    esModel.setSkuTitle(skuTitle.fragments()[0].string());
                }

                esModels.add(esModel);
            }
        result.setProducts(esModels);
        //###############以上hits中获取################
        //2、 当前所有商品涉及到的所有属性信息  属性来源于聚合
        List<SearchResult.AttrVo>attrVos=new ArrayList<>();
        ParsedNested attr_agg = response.getAggregations().get("attr_agg");
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        for (Terms.Bucket bucket : attr_id_agg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            //1.得到属性的id
            long attrId = bucket.getKeyAsNumber().longValue();
            //2.得到属性的所有值
            List<String> attrValues = ((ParsedStringTerms) bucket.getAggregations().get("attr_value_agg")).getBuckets().stream().map(item -> {
                String keyAsString = item.getKeyAsString();
                return keyAsString;
            }).collect(Collectors.toList());
            //3.得到属性的名字
            String attrName = ((ParsedStringTerms) bucket.getAggregations().get("attr_name_agg")).getBuckets().get(0).getKeyAsString();

            attrVo.setAttrId(attrId);
            attrVo.setAttrName(attrName);
            attrVo.setAttrValue(attrValues);
            attrVos.add(attrVo);
        }
        result.setAttrs(attrVos);
        //3、当前所有商品涉及到的所有品牌信息
        List<SearchResult.BrandVo> brandVos=new ArrayList<>();
        ParsedLongTerms brand_agg = response.getAggregations().get("brand_agg");
        for (Terms.Bucket bucket : brand_agg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            //1、得到品牌的id
            long brandId = bucket.getKeyAsNumber().longValue();
            brandVo.setBrandId(brandId);
            //2. 得到品牌的名字
            String brand_name_agg = ((ParsedStringTerms) bucket.getAggregations().get("brand_name_agg")).getBuckets().get(0).getKeyAsString();
            brandVo.setBrandName(brand_name_agg);
            //3. 得到品牌的图片
            String brand_img_agg = ((ParsedStringTerms) bucket.getAggregations().get("brand_img_agg")).getBuckets().get(0).getKeyAsString();
            brandVo.setBrandImg(brand_img_agg);
            brandVos.add(brandVo);
        }
        result.setBrands(brandVos);
        //4、 当前所有商品涉及到的所有分类信息
        ParsedLongTerms catelog_agg = response.getAggregations().get("catelog_agg");
        ArrayList<SearchResult.CatelogVo> catelogVos = new ArrayList<>();
        List<? extends Terms.Bucket> buckets = catelog_agg.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            SearchResult.CatelogVo catelogVo = new SearchResult.CatelogVo();
            //得到分类id
            String keyAsString = bucket.getKeyAsString();
            catelogVo.setCatelogId(Long.parseLong(keyAsString));
            //得到分类名
            ParsedStringTerms catelog_name_agg = bucket.getAggregations().get("catelog_name_agg");
            String catelog_name = catelog_name_agg.getBuckets().get(0).getKeyAsString();
            catelogVo.setCatelogName(catelog_name);
            catelogVos.add(catelogVo);
        }
        result.setCatelogs(catelogVos);
        //###############以上从聚合信息中获取################
        //5、 分页信息-页码
        result.setPageNum(parm.getPageNum());
        //6、 分页信息-总记录数
        Long total = hits.getTotalHits().value;
        result.setTotal(total);
        //7、 分页信息-总页码
        int totalPage = (int) (total % EsConstant.PRODUCT_PAGESIZE == 0 ? total / EsConstant.PRODUCT_PAGESIZE : (total / EsConstant.PRODUCT_PAGESIZE + 1));
        result.setTotalPages(totalPage);

        return result;
    }
}