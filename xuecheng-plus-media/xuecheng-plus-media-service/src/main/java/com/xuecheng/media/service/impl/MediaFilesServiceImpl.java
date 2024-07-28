package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;

import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.IMediaFilesService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    //注入的bean都是代理对象
    @Autowired
    IMediaFilesService currentProxy;

    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Autowired
    MinioClient minioClient;

    @Autowired
    private MediaProcessMapper mediaProcessMapper;

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
    public RestResponse<Boolean> checkFile(String fileMd5) {
        //先查询数据库
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);

        if (mediaFiles != null){
            //获取参数信息
            String bucket = mediaFiles.getBucket();
            String filePath = mediaFiles.getFilePath();
            //如果数据库存在再查询 minio
            GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(filePath)
                    .build();
            //查询远程服务获取到一个流对象
            try {
                GetObjectResponse inputStream = minioClient.getObject(getObjectArgs);
                if (inputStream != null){
                    //文件已存在
                    inputStream.close();
                    return RestResponse.success(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //文件不存在
        return RestResponse.success(false);
    }

    @Override
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {

        //分块存储路径 md5前两位 为两个目录
        //根据md5
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5) + chunkIndex;
        //如果数据库存在再查询 minio
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(bucket_video)
                .object(chunkFileFolderPath)
                .build();
            //查询远程服务获取到一个流对象
            try {

                GetObjectResponse inputStream = minioClient.getObject(getObjectArgs);
                if (inputStream != null){
                    //文件已存在
                    //关闭minio流
                    inputStream.close();
                    return RestResponse.success(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        //文件不存在
        return RestResponse.success(false);
    }


    @Override
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath) {

        //文件名
        String filename = uploadFileParamsDto.getFilename();
        //先得到扩展名
        String extension = filename.substring(filename.lastIndexOf("."));

        //得到mimeType
        String mimeType = getMimeType(extension);

        //子目录
        String defaultFolderPath = getDefaultFolderPath();
        //文件的md5值
        String fileMd5 = getFileMd5(new File(localFilePath));
        String objectName = defaultFolderPath+fileMd5+extension;
        //上传文件到minio
        boolean result = addMediaFilesToMinIO(localFilePath, mimeType, bucket_mediafiles, objectName);
        if(!result){
            XueChengPlusException.cast("上传文件失败");
        }
        //入库文件信息
        MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_mediafiles, objectName);
        if(mediaFiles==null){
            XueChengPlusException.cast("文件上传后保存信息失败");
        }
        //准备返回的对象
        UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles,uploadFileResultDto);

        return uploadFileResultDto;
    }


    /**
     *  上传分块
     * @param fileMd5 文件md5值
     * @param chunk 分别序号
     * @param localChunkFilePath 分块文件路径
     * @return
     */
    @Override
    public RestResponse uploadChunk(String fileMd5, int chunk,String localChunkFilePath) {
        //分块文件的路径
        String chunkFilePath = getChunkFileFolderPath(fileMd5) + chunk;
        //获取mimeType
        String mimeType = getMimeType(null);
//        String mimeType = getMimeType(null);
        //将分块文件上传到minio
        boolean b = addMediaFilesToMinIO(localChunkFilePath,mimeType,bucket_video,chunkFilePath);
        if (!b){
            //上传失败
            return RestResponse.validfail(false,"上传分块文件失败");
        }
        //上传失败
        return RestResponse.success(true);

    }

    @Override
    public RestResponse mergeChunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) throws NoSuchAlgorithmException, InvalidKeyException {
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
//        ServerSideEncryptionCustomerKey srcSsec =
//                new ServerSideEncryptionCustomerKey(
//                        new SecretKeySpec(
//                                "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8), "AES"));

        //找到分块文件调用minio进行文件合并
        List<ComposeSource> sources = Stream.iterate(0, i -> ++i).limit(chunkTotal).map(i ->
                ComposeSource.builder()
                        .bucket(bucket_video)
                        .object(chunkFileFolderPath + i)
//                        .ssec(srcSsec)
                        .build()
        ).collect(Collectors.toList());

        //源文件的名称
        String filename = uploadFileParamsDto.getFilename();
        //扩展名
        String extension = filename.substring(filename.lastIndexOf("."));
        //合并后文件的objectName
        String objectName = getFilePathByMd5(fileMd5, extension);


        try {
            //=========================合并文件,
//            GetObjectResponse getObjectResponse = minioClient.getObject(GetObjectArgs.builder()
//                            .bucket(bucket_video)
//                            .object(objectName)
//                    .build());
//            if (getObjectResponse == null) {
//                指定合并后端objectName等信息

//            ServerSideEncryption sse =
//                    new ServerSideEncryptionCustomerKey(
//                            new SecretKeySpec(
//                                    "12345678912345678912345678912345".getBytes(StandardCharsets.UTF_8), "AES"));
                ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
                        .bucket(bucket_video)
                        .object(objectName) //最终合并后的文件
                        .sources(sources)
//                        .sse(sse)
                        .build();
                //minio默认的分块文件大小为5M
                minioClient.composeObject(composeObjectArgs);
//            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("合并文件出错，bucket：{}，objectName：{}，错误信息：{}",bucket_video,objectName,e.getMessage());
            return RestResponse.validfail(false,"合并文件出错");
        }

        //===========================校验合并后的和源文件是否一致
        File file = downloadFileFormMinIO(bucket_video, objectName);
        //计算合并后的md5
        try (FileInputStream fileInputStream = new FileInputStream(file)){
            String mergeFile_Md5 = DigestUtils.md5Hex(fileInputStream);
            //比较原始md5和合并后的md5
            if (!fileMd5.equals(mergeFile_Md5)){
                log.error("校验合并文件md5值不一致，原始文件：{}，和并文件：{}",fileMd5,mergeFile_Md5);
                return RestResponse.validfail(false,"文件校验失败");
            }
            //文件大小
            uploadFileParamsDto.setFileSize(file.length());
        }catch (Exception e){
            return RestResponse.validfail(false,"文件校验失败");
        }
        //=============================将文件信息入库
        MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_video, objectName);
        if (mediaFiles == null){
            return RestResponse.validfail(false,"文件入库失败");
        }
        //==============================清理分块文件
        clearChunkFiles(chunkFileFolderPath,chunkTotal);
        return RestResponse.success(true);
    }

    /**
     * 清除分块文件
     * @param chunkFileFolderPath 分块文件路径
     * @param chunkTotal 分块文件总数
     */
    private void clearChunkFiles(String chunkFileFolderPath,int chunkTotal){
        Iterable<DeleteObject> objects = Stream.iterate(0,i -> ++i).limit(chunkTotal).map(i -> new DeleteObject(chunkFileFolderPath.concat(Integer.toString(i)))).collect(Collectors.toList());
        Iterable<Result<DeleteError>> results = minioClient.removeObjects(RemoveObjectsArgs.builder()
                .bucket(bucket_video)
                .objects(objects)
                .build());

        //需要遍历一下 参能完成删除     惰性迭代器包含对象删除状态。
//        results.forEach(r->{
//            DeleteError deleteError = null;
//            try {
//                deleteError = r.get();
//            } catch (Exception e) {
//                e.printStackTrace();
//                log.error("清楚分块文件失败,objectname:{},错误信息：{}",deleteError.objectName(),e.getMessage());
//            }
//        });
        //官网解释
        // Iterable<Result<DeleteError>> - Lazy iterator contains object removal status. 惰性迭代器包含对象删除状态。
        for (Result<DeleteError> result : results) {
            DeleteError error = null;
            try {
                error = result.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Error in deleting object" + error.objectName() + ";" + error.message());
            }
    }
    /**
     *      从minio 下载文件
     * @param bucket
     * @param objectName
     * @return
     */
    public File downloadFileFormMinIO(String bucket,String objectName){
        //临时文件
        File minioFile = null;
        FileOutputStream outputStream = null;
        try {
            GetObjectResponse stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build()
            );
            //创建临时文件
            minioFile = File.createTempFile("minio", ",merge");
            outputStream = new FileOutputStream(minioFile);
            IOUtils.copy(stream,outputStream);
            return minioFile;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
    //将文件上传到minIO

    /**
     *
     * @param bucket 桶目录
     * @param localFilePath 本地文件路径
     * @return
     */
    public boolean addMediaFilesToMinIO(String localFilePath,String mimeType,String bucket, String objectName){
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
            log.debug("上传文件到minio成功,bucket:{},objectName:{}",bucket,objectName);
            return true;
        } catch (Exception e){
            log.error("上传文件出错,bucket:{},objectName:{},错误信息:{}",bucket,objectName,e.getMessage());
        }
        return false;
    }

    @Transactional
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
            //记录待处理任务
            //通过mimeType 判断如果是avi视频写入带处理任务
            addWaitingTask(mediaFiles);
            //向MediaProcess 插入记录
        }
        return mediaFiles;
    }


    //根据扩展名获取mimeType
    private String getMimeType(String extension){
        if (extension == null)
            extension = "";
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType 字节流
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

    //根据文件md5 生成文件路径
    private String getChunkFileFolderPath(String fileMd5){
        return fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5 + "/" + "chunk" + "/";
    }

    //根据文件md5 生成文件路径
    private String getFilePathByMd5(String fileMd5,String fileExt){
        return fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }

    /**
     * 添加待处理任务
     * @param mediaFiles 媒资文件信息
     */
    private void addWaitingTask(MediaFiles mediaFiles){
        //获取文件的mimeType
        String filename = mediaFiles.getFilename();
        String extension = filename.substring(filename.lastIndexOf("."));
        String mimeType = getMimeType(extension);

        if (mimeType.equals("video/x-msvideo")){
            MediaProcess mediaProcess = new MediaProcess();
            BeanUtils.copyProperties(mediaFiles,mediaProcess);
            //状态是未处理
            mediaProcess.setStatus("1");//1是未处理
            mediaProcess.setCreateDate(LocalDateTime.now());
            mediaProcess.setFailCount(0);//失败次数
            mediaProcessMapper.insert(mediaProcess);
        }

        //通过mimeType判断如果是avi 视频写入待处理任务
    }

}
