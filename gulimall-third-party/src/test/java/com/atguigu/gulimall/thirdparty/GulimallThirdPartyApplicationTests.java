package com.atguigu.gulimall.thirdparty;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.PutObjectRequest;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.gulimall.thirdparty.component.SmsComponent;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class GulimallThirdPartyApplicationTests {

    @Resource
    OSSClient ossClient;

    @Autowired
    SmsComponent smsComponent;

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

    @Test
     void sendSms(){
        String host = "https://smssend.shumaidata.com";
        String path = "/sms/send";
        String method = "POST";
        String appcode = "4f7292af19c348a38840e47b5b504d36";
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        Map<String, String> querys = new HashMap<String, String>();
        querys.put("receive", "13007632818");
        querys.put("tag", "8699");
        querys.put("templateId", "M4F8845237");
        Map<String, String> bodys = new HashMap<String, String>();


        try {
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            System.out.println(response.toString());
            System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            e.printStackTrace();
        }
     }


     @Test
     public void testSendCode(){
         String s = smsComponent.sendSmsCode("13007632818", "66666");
     }

}
