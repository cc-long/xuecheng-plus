package com.xuecheng.content;

import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * @author CCL
 * @version 1.0
 * @description 远程调用媒资服务
 * @createTime 2024-08-22 21:54
 **/
@SpringBootTest
public class FeignUploadTest {

    @Autowired
    MediaServiceClient mediaServiceClient;

    @Test
    public void test() throws IOException {

        //将file转成MultipartFile
        File file = new File("E:\\IDEA\\dema\\downloadDemo\\xuecheng\\xuecheng-plus\\xuecheng-plus-content\\xuecheng-plus-content-service\\src\\test\\resources\\templates\\121.html");
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);

        String upload = mediaServiceClient.upload(multipartFile, "course/121.html");
        if (upload == null){
            System.out.println("无可用媒资服务，走降级逻辑");
        }
    }
}
