package com.xuecheng.content.feignclient;

import org.springframework.web.multipart.MultipartFile;

/**
 * @author CCL
 * @version 1.0
 * @description TODO
 * @createTime 2024-08-24 15:09
 **/

/**
 *
 */
public class MediaServiceClientFallback implements MediaServiceClient{
    @Override
    public String upload(MultipartFile filedata, String objectName) {

        return null;
    }
}
