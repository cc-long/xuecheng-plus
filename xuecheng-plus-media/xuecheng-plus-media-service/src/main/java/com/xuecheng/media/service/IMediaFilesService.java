package com.xuecheng.media.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;


/**
 * <p>
 * 媒资信息 服务类
 * </p>
 *
 * @author CCL
 * @since 2024-07-13
 */
public interface IMediaFilesService {
    /**
     * 媒体资源列表
     * @param companyId
     * @param pageParams
     * @param queryMediaParamsDto
     * @return
     */
    PageResult<MediaFiles> queryMediaFiles(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto);

    /**
     * 上传文件
     * @param companyId 机构id
     * @param uploadFileParamsDto 文件信息
     * @param localFilePath 文件本地路径
     * @return UploadFileResultDto
     */
    UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath);


    /**
     * 检查文件是否存在
     * @param fileMd5 文件的md5
     * @return
     */
    RestResponse<Boolean> checkFile(String fileMd5);


    /**
     * 检查分块文件是否存在
     * @param fileMd5 分块文件的md5值
     * @param chunkIndex 分块的索引
     * @return
     */
    RestResponse<Boolean> checkChunk(String fileMd5,int chunkIndex);

    /**
     * 上传分块文件
     * @param fileMd5 分块文件md5
     * @param chunk 分块号
     * @param localChunkFilePath  本地文件路径
     * @return
     */
    RestResponse uploadChunk(String fileMd5,int chunk,String localChunkFilePath);

    /**
     * 合并分块文件
     * @param companyId 机构id
     * @param fileMd5 文件md5
     * @param chunkTotal 分块总数
     * @param uploadFileParamsDto 文件信息
     * @return
     */
    public RestResponse mergeChunks(Long companyId,String fileMd5,int chunkTotal,UploadFileParamsDto uploadFileParamsDto) throws NoSuchAlgorithmException, InvalidKeyException;

    /**
     *
     * 注入的bean都是代理对象    addMediaFilesToDb用作代理对象
     * @param companyId
     * @param fileMd5
     * @param uploadFileParamsDto
     * @param bucket
     * @param objectName
     * @return
     */
    public MediaFiles addMediaFilesToDb(Long companyId,String fileMd5,UploadFileParamsDto uploadFileParamsDto,String bucket,String objectName);
}
