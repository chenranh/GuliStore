package com.atguigu.gulimall.product.feign;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.sound.midi.Soundbank;

/**
 * @title: test
 * @Author yuke
 * @Date: 2020-09-30 10:22
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class test {


    public Integer num;

    public Integer getNum() {
        return num+1;
    }

    public static void main(String[] args) {
        test test = new test();
        System.out.println(test.getNum());
    }
}
