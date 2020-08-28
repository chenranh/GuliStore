package com.atguigu.gulimall.thirdparty;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.PutObjectRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

@SpringBootTest
class GulimallThirdPartyApplicationTests {

    @Resource
    OSSClient ossClient;

    @Test
    void contextLoads() {
    }

    /**
     * 1、引入oss-starter
     * 2、配置key，endpoint相关信息即可
     * 3、使用OSSClient 进行相关操作
     */
    @Test
    void testUpload2() throws FileNotFoundException {



        FileInputStream fileInputStream = new FileInputStream("C:\\Users\\Tiddler\\Desktop\\微信图片_20200826074943.jpg");


        PutObjectRequest putObjectRequest = new PutObjectRequest("gulimall-yuke", "hahahaha.jpg", fileInputStream);




        ossClient.putObject(putObjectRequest);


        ossClient.shutdown();
        System.out.println("success");
    }
}
