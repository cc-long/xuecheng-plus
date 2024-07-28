package com.xuecheng.media.service;

import com.xuecheng.media.model.po.MediaProcess;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author CCL
 * @since 2024-07-28
 */
public interface IMediaProcessService extends IService<MediaProcess> {

    List<MediaProcess> getMediaProcessList(int shardTotal,int shardIndex,int count);

    /**
     * 开启一个任务
     * @param id 任务id
     * @return 更新记录数
     */
    int startTask(long id);

    /**
     * 保存任务结果
     * @param taskId 任务id
     * @param status 任务状态
     * @param fileId 文件id
     * @param url url
     * @param errorMsg 错误信息
     * @return void
     */
    void saveProcessFinishStatus(Long taskId,String status,String fileId,String url,String errorMsg);
}
