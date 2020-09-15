package com.atguigu.gulimall.search.controller;

import com.atguigu.gulimall.search.service.MallSearchService;
import com.atguigu.gulimall.search.vo.SearchParm;
import com.atguigu.gulimall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class SearchController {

    @Autowired
    MallSearchService mallSearchService;

    /**
     * 自动将页面提交过来的所有请求参数封装对象
     * @param parm
     * @return
     */
    @GetMapping("/list.html")
    public Object listPage(SearchParm parm, Model model, HttpServletRequest request){
        String queryString = request.getQueryString();
        parm.set_queryString(queryString);
        //根据传递过来的页面的查询参数，去es中检索商品
        SearchResult result = mallSearchService.search(parm);
        model.addAttribute("result",result);
        return result;
    }
}
