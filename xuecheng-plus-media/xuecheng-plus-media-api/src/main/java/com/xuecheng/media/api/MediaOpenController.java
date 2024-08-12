package com.xuecheng.media.api;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.IMediaFilesService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author CCL
 * @version 1.0
 * @description TODO
 * @createTime 2024-08-04 21:53
 **/
@Api(value = "媒资文件管理接口",tags = "媒资文件管理接口")
@RestController
@RequestMapping("/open")
public class MediaOpenController {

    @Autowired
    private IMediaFilesService mediaFilesService;

    @ApiOperation("预览文件")
    @GetMapping("/preview/{mediaId}")
    public RestResponse<String> getPlayUrlByMediaId(@PathVariable String mediaId){
        //
        MediaFiles mediaFiles = mediaFilesService.getFileById(mediaId);
        if (mediaFiles == null)
        {
            XueChengPlusException.cast("找不到视频");
        }
            String url = mediaFiles.getUrl();
            if (StringUtils.isEmpty(url)){
                XueChengPlusException.cast("该视频正在处理中");
            }

        return RestResponse.success(mediaFiles.getUrl());
    }
}
