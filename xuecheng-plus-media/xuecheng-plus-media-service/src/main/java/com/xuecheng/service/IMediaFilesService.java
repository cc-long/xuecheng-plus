package com.xuecheng.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.model.dto.QueryMediaParamsDto;
import com.xuecheng.model.dto.UploadFileParamsDto;
import com.xuecheng.model.po.MediaFiles;

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
    public UploadFileParamsDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath);
}
