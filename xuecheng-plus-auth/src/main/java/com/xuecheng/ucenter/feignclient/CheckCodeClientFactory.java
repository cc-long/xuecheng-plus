package com.xuecheng.ucenter.feignclient;

import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author CCL
 * @version 1.0
 * @description TODO
 * @createTime 2024-09-04 23:11
 **/
@Slf4j
@Component
public class CheckCodeClientFactory implements FallbackFactory {
    @Override
    public CheckCodeClient create(Throwable throwable) {
        return new CheckCodeClient() {
            @Override
            public Boolean verify(String key, String code) {
                log.debug("调用验证码服务熔断异常：{}",throwable.getMessage());
                return null;
            }
        }
                ;
    }
}
