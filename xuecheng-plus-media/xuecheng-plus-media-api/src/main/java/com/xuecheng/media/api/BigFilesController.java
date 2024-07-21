package com.xuecheng.media.api;

import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.service.IMediaFilesService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

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
    public RestResponse<Boolean> checkFile(@RequestParam("fileMd5") String fileMd5) throws Exception{

        return mediaFilesService.checkFile(fileMd5);
    }

    @ApiOperation(value = "分块文件上传签检查文件")
    @PostMapping("/upload/checkchunk")
    public RestResponse<Boolean> checkChunk(
            @RequestParam("fileMd5") String fileMd5,
            @RequestParam("chunk") int chunkIndex) throws Exception{

        return mediaFilesService.checkChunk(fileMd5,chunkIndex);
    }

    @ApiOperation(value = "上传分块文件")
    @PostMapping("/upload/uploadchunk")
    public RestResponse uploadChunk (@RequestParam("file")MultipartFile file,
                                     @RequestParam("fileMd5") String fileMd5,
                                     @RequestParam("chunk") int chunk) throws Exception{
        //创建一个临时文件
        File tempFile = File.createTempFile("minio", ".temp");
        file.transferTo(tempFile);

        //文件路径
        String absolutePath  = tempFile.getAbsolutePath();

         return mediaFilesService.uploadChunk(fileMd5, chunk, absolutePath);
    }

    @ApiOperation(value = "文件合并")
    @PostMapping("/upload/mergechunks")
    public RestResponse mergeChunks(@RequestParam("fileMd5") String fileMd5,
                                    @RequestParam("fileName") String fileName,
                                    @RequestParam("chunkTotal") int chunkTotal
                                    ) throws Exception{
        Long companyId = 1232141425L;
        //文件信息对象
        UploadFileParamsDto uploadFileParamsDto = new UploadFileParamsDto();
        uploadFileParamsDto.setFilename(fileName);
        uploadFileParamsDto.setTags("视频文件");
        uploadFileParamsDto.setFileType("001002");
        return mediaFilesService.mergeChunks(companyId, fileMd5, chunkTotal, uploadFileParamsDto);
    }
}
