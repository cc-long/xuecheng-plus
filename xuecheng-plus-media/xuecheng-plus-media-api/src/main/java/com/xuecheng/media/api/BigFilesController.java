package com.xuecheng.media.api;

import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.service.IMediaFilesService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author CCL
 * @version 1.0
 * @description TODO
 * @createTime 2024-07-17 22:54
 **/
@Api(value = "大文件上传接口",tags = "大文件上传接口")
@RestController
public class BigFilesController {

    @Autowired
    private IMediaFilesService mediaFilesService;

    @ApiOperation(value = "文件上传签检查文件")
    @PostMapping("/upload/checkfile")
    public RestResponse<Boolean> checkFile(
            @RequestParam("fileMd5") String fileMd5
    ) throws Exception{

        return mediaFilesService.checkFile(fileMd5);
    }

    @ApiOperation(value = "分块文件上传签检查文件")
    @PostMapping("/upload/checkChunk")
    public RestResponse<Boolean> checkChunk(
            @RequestParam("fileMd5") String fileMd5,
            @RequestParam("chunk") int chunkIndex) throws Exception{

        return mediaFilesService.checkChunk(fileMd5,chunkIndex);
    }
}
