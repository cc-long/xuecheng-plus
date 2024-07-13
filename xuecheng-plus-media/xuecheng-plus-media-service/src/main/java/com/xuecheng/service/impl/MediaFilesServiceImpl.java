package com.xuecheng.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.model.dto.QueryMediaParamsDto;
import com.xuecheng.model.dto.UploadFileParamsDto;
import com.xuecheng.model.po.MediaFiles;
import com.xuecheng.mapper.MediaFilesMapper;
import com.xuecheng.service.IMediaFilesService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * <p>
 * 媒资信息 服务实现类
 * </p>
 *
 * @author CCL
 * @since 2024-07-13
 */
@Service
@Slf4j
public class MediaFilesServiceImpl extends ServiceImpl<MediaFilesMapper, MediaFiles> implements IMediaFilesService {

    @Autowired
    private MediaFilesMapper mediaFilesMapper;

    @Autowired
    private MinioClient minioClient;

    //存储普通文件的桶
    @Value("${minio.bucket.files}")
    private String bucket_mediafiles;

    //存储视频文件的桶
    @Value("${minio.bucket.video.files}")
    private String bucket_video;
    @Override
    public PageResult<MediaFiles> queryMediaFiles(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(companyId != null,MediaFiles::getCompanyId,companyId);

        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(),pageParams.getPageSize());
        Page<MediaFiles> mediaFilesPage = mediaFilesMapper.selectPage(page, queryWrapper);

        PageResult<MediaFiles> mediaFilesPageResult = new PageResult<>(mediaFilesPage.getRecords(), mediaFilesPage.getTotal(), pageParams.getPageNo(), pageParams.getPageSize());
        return mediaFilesPageResult;
    }

    @Override
    public UploadFileParamsDto uploadFile(Long companyId,UploadFileParamsDto uploadFileParamsDto, String localFilePath) {

        //文件名
        String filename = uploadFileParamsDto.getFilename();


        uploadMinIO(bucket_mediafiles,localFilePath);

        return null;
    }

    //将文件上传到minIO

    /**
     *
     * @param bucket
     * @param localFilePath
     * @return
     */
    public boolean uploadMinIO(String bucket,String localFilePath){
        try {
            //得到mimeType
            String extension = localFilePath.substring(localFilePath.lastIndexOf("."));
            String mimeType = getMimeType(extension);

            //objectName：上传的文件路径 + 文件名称 + 文件后缀
            //             上传的文件路径是当天日期 + 文件名称是文件的md5值 + 文件后缀是mimeType
            String fileMd5 = getFileMd5(new File(localFilePath));
            String objectName = getDefaultFolderPath() + fileMd5 + extension;
            //上传文件
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            //minio的桶 路径
                            .bucket(bucket)
                            //本地文件路径
                            .filename(localFilePath)
                            //上传minio的路径
                            .object(objectName)
                            //设置文件类型 mimeType
                            .contentType(mimeType)
                            .build()
            );
            return true;
        } catch (Exception e){
            log.error("上传文件出错，bucket:{},错误信息:{}",bucket,e.getMessage());
            XueChengPlusException.cast(e.getMessage());
        }
        return false;
    }

    //根据扩展名获取mimeType
    private String getMimeType(String extension){
        if (extension == null)
            extension = "";
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (extensionMatch != null){
            mimeType = extensionMatch.getMimeType();
        }
        return mimeType;
    }

    //获取文件默认存储目录路径  年/月/日
    private String getDefaultFolderPath(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String folder = sdf.format(new Date()).replace("-", "/") + "/";
        //2024-07-14        2024/07/14
        return folder;
    }

    private String getFileMd5(File file){
        try (FileInputStream fileInputStream = new FileInputStream(file)){
            String fileMd5 = DigestUtils.md5Hex(fileInputStream);
            return fileMd5;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
