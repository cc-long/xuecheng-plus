package com.xuecheng.controller;


import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.model.dto.QueryMediaParamsDto;
import com.xuecheng.model.dto.UploadFileParamsDto;
import com.xuecheng.model.po.MediaFiles;
import com.xuecheng.service.IMediaFilesService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 媒资信息 前端控制器
 * </p>
 *
 * @author CCL
 * @since 2024-07-13
 */
@Controller
@RequestMapping("/media-files")
public class MediaFilesController {

    @Autowired
    private IMediaFilesService mediaFilesService;

    @ApiOperation("媒资列表查询接口")
    @PostMapping("/files")
    public PageResult<MediaFiles> list(PageParams pageParams, @RequestBody QueryMediaParamsDto queryMediaParamsDto){
        Long companyId = 1232141425L;
        return mediaFilesService.queryMediaFiles(companyId,pageParams,queryMediaParamsDto);
    }

    @ApiOperation("上传图片")
    @RequestMapping(value = "upload/coursefile",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadFileParamsDto upload(@RequestParam("filedata") MultipartFile filedata){
        return null;
    }

}
