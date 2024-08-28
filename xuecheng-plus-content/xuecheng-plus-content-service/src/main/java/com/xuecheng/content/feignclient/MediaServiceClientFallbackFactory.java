package com.xuecheng.content.feignclient;

import com.xuecheng.base.exception.XueChengPlusException;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author CCL
 * @version 1.0
 * @description TODO
 * @createTime 2024-08-24 15:13
 **/
@Slf4j
@Component
public class MediaServiceClientFallbackFactory implements FallbackFactory<MediaServiceClient>{
    @Override
    public MediaServiceClient create(Throwable throwable) {
        return new MediaServiceClient() {
            //当发生熔断，上层服务调用此方法执行降级逻辑
            @Override
            public String upload(MultipartFile filedata, String objectName) throws IOException {
                log.debug("远程调用上传文件的接口发生熔断：{}",throwable.toString(),throwable);
                return null;
            }
        };
    }
}
