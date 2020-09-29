package com.atguigu.gulimallcart.controller;

import com.atguigu.gulimallcart.interceptor.CartInterceptor;
import com.atguigu.gulimallcart.service.CartService;
import com.atguigu.gulimallcart.vo.Cart;
import com.atguigu.gulimallcart.vo.CartItem;
import com.atguigu.gulimallcart.vo.UserInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Controller
public class CartController {

    private final String PATH = "redirect:http://cart.gulimall.com/cart.html";

    @Autowired
    CartService cartService;


    /**
     * 当前用户的购物车里面所有的购物项 用于订单服务远程调用
     * @return
     */
    @ResponseBody
    @GetMapping("/currentUserCartItems")
    public List<CartItem> getCurrentUserCartItems(){

        return cartService.getUserCartItems();
    }

    /**
     * 勾选状态
     * @param skuId
     * @param check
     * @return
     */
    @GetMapping("checkItem.html")
    public String checkItem(@RequestParam("skuId") Long skuId, @RequestParam("check") Integer check){
        cartService.checkItem(skuId, check);
        return PATH;
    }

    /**
     * 删除购物车商品
     * @param skuId
     * @return
     */
    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId){
        cartService.deleteItem(skuId);
        return PATH;
    }

    /**
     * 增减购物车商品
     * @param skuId
     * @param num
     * @return
     */
    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num){
        cartService.changeItemCount(skuId, num);
        return PATH;
    }


    /**
     * 添加商品到购物车
     * RedirectAttributes
     * ra.addFlashAttribute();将数据放在session里面可以在页面取出，但是只能取一次
     * ra.addAttribute("skuId", skuId); 将数据放在url后面
     *
     * @return
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num, RedirectAttributes ra) throws ExecutionException, InterruptedException {
        cartService.addToCart(skuId, num);
        ra.addAttribute("skuId", skuId);
        // 重定向到成功页面
        return "redirect:http://cart.gulimall.com/addToCartSuccess.html";
    }


    /**
     * 跳转到成功页
     *
     * @param skuId
     * @param model
     * @return
     */
    @GetMapping("/addToCartSuccess.html")
    public String addToCartSuccessPage(@RequestParam(value = "skuId", required = false) Object skuId, Model model) {
        CartItem cartItem = null;
        // 然后在查一遍 购物车
        if (skuId == null) {
            model.addAttribute("item", null);
        } else {
            try {
                cartItem = cartService.getCartItem(Long.parseLong((String) skuId));
            } catch (NumberFormatException e) {
                log.warn("恶意操作! 页面传来非法字符.");
            }
            model.addAttribute("item", cartItem);
        }
        return "success";
    }


    /**
     * 浏览器有一个cookie：user-key标识用户身份，一个月后过期
     * 如果第一次使用jd的购物车功能 都会有一个临时的用户身份
     * 浏览器以后保存，每次访问都会带上这个cookie
     * <p>
     * 登录：session中有
     * 没登陆：按照cookie里面带来user-key来做
     * 第一次如果没有临时用户 帮忙创建一个临时用户
     *
     * @return
     */
    @GetMapping("/cart.html")
    public String cartListPage(Model model) throws ExecutionException, InterruptedException {
        Cart cart = cartService.getCart();
        model.addAttribute("cart", cart);
        return "cartList";
    }


}
