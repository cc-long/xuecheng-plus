package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;

import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.IMediaFilesService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * <p>
 * 媒资信息 服务实现类
 * </p>
 *
 * @author CCL
 * @since 2024-07-13
 */
@Slf4j
@Service
public class MediaFilesServiceImpl extends ServiceImpl<MediaFilesMapper, MediaFiles> implements IMediaFilesService {

    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Autowired
    MinioClient minioClient;

    //存储普通文件的桶
    @Value("${minio.bucket.files}")
    String bucket_mediafiles;

    //存储视频文件的桶
    @Value("${minio.bucket.videofiles}")
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
    @Transactional
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath) {

        //文件名
        String filename = uploadFileParamsDto.getFilename();
        //得到mimeType
        String extension = filename.substring(filename.lastIndexOf("."));
        String mimeType = getMimeType(extension);

        //objectName 上传文件地址：上传的文件路径 + 文件名称 + 文件后缀
        //             上传的文件路径是当天日期 + 文件名称是文件的md5值 + 文件后缀是mimeType
        String fileMd5 = getFileMd5(new File(localFilePath));
        String objectName = getDefaultFolderPath() + fileMd5 + extension;

        boolean result = addMinIO(bucket_mediafiles, localFilePath, mimeType, objectName);
        if (!result)
            XueChengPlusException.cast("上传文件失败");
        //入库文件信息
        MediaFiles mediaFiles = addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_mediafiles, objectName);
        //准备返回的对象
        UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles,uploadFileResultDto);

        return uploadFileResultDto;
    }

    public MediaFiles addMediaFilesToDb(Long companyId,String fileMd5,UploadFileParamsDto uploadFileParamsDto,String bucket,String objectName){
        //将文件信息保存到数据库
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles == null) {
            mediaFiles = new MediaFiles();
            BeanUtils.copyProperties(uploadFileParamsDto,mediaFiles);
            //文件id
            mediaFiles.setId(fileMd5);
            //机构id
            mediaFiles.setCompanyId(companyId);
            //上传的桶
            mediaFiles.setBucket(bucket);
            //filePath
            mediaFiles.setFilePath(objectName);
            //file_id
            mediaFiles.setFileId(fileMd5);
            //url
            mediaFiles.setUrl("/" + bucket + "/" + objectName);
            //上传时间
            mediaFiles.setCreateDate(LocalDateTime.now());
            //状态
            mediaFiles.setStatus("1");
            //审核状态
            mediaFiles.setAuditStatus("002003");
            int insert = mediaFilesMapper.insert(mediaFiles);
            if (insert <= 0){
                log.debug("向数据库保存文件失败，bucket:{},objectName:{}",bucket,objectName);
                return null;
            }
        }
        return mediaFiles;
    }

    //将文件上传到minIO

    /**
     *
     * @param bucket 桶目录
     * @param localFilePath 本地文件路径
     * @param mimeType 文件后缀类型
     * @param objectName 上传文件地址
     * @return
     */
    public boolean addMinIO(String bucket,String localFilePath,String mimeType,String objectName){
        try {
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
