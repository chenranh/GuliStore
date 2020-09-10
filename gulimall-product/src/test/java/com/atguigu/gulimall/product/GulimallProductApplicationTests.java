package com.atguigu.gulimall.product;


import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.service.BrandService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
class GulimallProductApplicationTests {
    @Autowired
    BrandService brandService;

    @Autowired
    StringRedisTemplate redisTemplate;

//    @Autowired
//    RedissonClient redissonClient;
//
//
//    @Test
//    void testredisson(){
//        System.out.println(redissonClient);
//    }


    //测试redis
    @Test
    void testRedis(){
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        //保存
        ops.set("helloo","word"+ UUID.randomUUID().toString());
    }


    @Test
    void contextLoads() {
        BrandEntity brandEntity = new BrandEntity();
//        brandEntity.setName("华为");
//        brandService.save(brandEntity);
//        System.out.println("保存成功");

        List<BrandEntity> brand_id = brandService.list(new QueryWrapper<BrandEntity>().eq("brand_id", 1L));
        for (BrandEntity entity : brand_id) {
            System.out.println(brand_id);
        }

    }

    //可以在springboot中使用的阿里oss存储
//    @Test
//    void testUpload() throws FileNotFoundException {
//        // Endpoint以杭州为例，其它Region请按实际情况填写。
//        String endpoint = "oss-cn-hangzhou.aliyuncs.com";
//// 阿里云主账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建RAM账号。
//        String accessKeyId = "LTAI4GAmNKXWh84DsJoMZWny";
//        String accessKeySecret = "QrCvUCvJuboUkIdUDH2lWiv707VwMR";
//
//// 创建OSSClient实例。
//        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
//
//// 创建PutObjectRequest对象。
////  上传字符串      String content = "Hello OSS";
//        //测试上传图片
//        FileInputStream fileInputStream = new FileInputStream("C:\\Users\\Tiddler\\Desktop\\微信图片_20200826074943.jpg");
//
//        // <yourObjectName>表示上传文件到OSS时需要指定包含文件后缀在内的完整路径，例如abc/efg/123.jpg。
////       PutObjectRequest putObjectRequest = new PutObjectRequest("<yourBucketName>", "<yourObjectName>", new ByteArrayInputStream(content.getBytes()));
//
//        //测试上传图片
//        PutObjectRequest putObjectRequest = new PutObjectRequest("gulimall-yuke", "cat.jpg", fileInputStream);
//
//
//// 如果需要上传时设置存储类型与访问权限，请参考以下示例代码。
//// ObjectMetadata metadata = new ObjectMetadata();
//// metadata.setHeader(OSSHeaders.OSS_STORAGE_CLASS, StorageClass.Standard.toString());
//// metadata.setObjectAcl(CannedAccessControlList.Private);
//// putObjectRequest.setMetadata(metadata);
//
//// 上传字符串。
//        ossClient.putObject(putObjectRequest);
//
//// 关闭OSSClient。
//        ossClient.shutdown();
//        System.out.println("success");
//    }
//
//    //分布式使用的存储
//
//    /**
//     * 1、引入oss-starter
//     * 2、配置key，endpoint相关信息即可
//     * 3、使用OSSClient 进行相关操作
//     * 4、在yml文件里配置好了endpoint, accessKeyId, accessKeySecret
//     */
//    @Test
//    void testUpload2() throws FileNotFoundException {
//
//
//
//        FileInputStream fileInputStream = new FileInputStream("C:\\Users\\Tiddler\\Desktop\\微信图片_20200826074943.jpg");
//
//
//        PutObjectRequest putObjectRequest = new PutObjectRequest("gulimall-yuke", "cat2.jpg", fileInputStream);
//
//
//
//
//        ossClient.putObject(putObjectRequest);
//
//
//        ossClient.shutdown();
//        System.out.println("success");
//    }


}
