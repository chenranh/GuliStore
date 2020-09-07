package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.forwebvo.Catelog2VO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

/**
 * @title: IndexController
 * @Author yuke
 * @Date: 2020-09-07 14:24
 */
@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @GetMapping({"/", "index.html"})
    public String indexPage(Model model) {
        // 查出所有一级数据展示在页面
        List<CategoryEntity> categoryEntities = categoryService.getLevel1Category();

        model.addAttribute("categorys", categoryEntities);
        //视图解析器进行拼串
        //classpath:/templates/+返回值+ .html
        return "index";
    }

    //index/catalog.json
    @GetMapping("index/catalog.json")
    public Map<String, List<Catelog2VO>> getCataLogJson() {

        Map<String, List<Catelog2VO>> cataLogJson = categoryService.getCataLogJson();

        return cataLogJson;
    }

}
