package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessHistoryMapper;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.po.MediaProcessHistory;
import com.xuecheng.media.service.IMediaProcessService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author CCL
 * @since 2024-07-28
 */
@Service
public class MediaProcessServiceImpl extends ServiceImpl<MediaProcessMapper, MediaProcess> implements IMediaProcessService {

    @Autowired
    private MediaProcessMapper mediaProcessMapper;

    @Autowired
    private MediaFilesMapper mediaFilesMapper;

    @Autowired
    private MediaProcessHistoryMapper mediaProcessHistoryMapper;

    @Override
    public List<MediaProcess> getMediaProcessList(int shardTotal, int shardIndex, int count) {
        return mediaProcessMapper.selectListByShardIndex(shardTotal,shardIndex,count);
    }

    @Override
    public boolean startTask(long id) {
        int result = mediaProcessMapper.startTask(id);
        return result<=0 ? false : true;
    }

    @Override
    @Transactional
    public void saveProcessFinishStatus(Long taskId, String status, String fileId, String url, String errorMsg) {

        //要更新的任务
        MediaProcess mediaProcess = mediaProcessMapper.selectById(taskId);
        if (mediaProcess == null) return;

        //如果任务执行失败
        if ("3".equals(status)){
            //更新MediaProcess表的状态
            mediaProcess.setStatus("3");
            mediaProcess.setFailCount(mediaProcess.getFailCount()+1);//失败次数+1
            mediaProcess.setErrormsg(errorMsg);
            mediaProcessMapper.updateById(mediaProcess);
            //更高效的更新方式
//            LambdaQueryWrapper<MediaProcess> queryWrapper = new LambdaQueryWrapper<>();
//            queryWrapper.eq(MediaProcess::getId,mediaProcess.getId());
//            mediaProcessMapper.update(mediaProcess,queryWrapper);
            return;
        }
        //==============如果任务执行成功
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileId);

        //更新mediaFile表 的url字段
        mediaFiles.setUrl(url);
        mediaFilesMapper.updateById(mediaFiles);

        //更新mediaProcess 表的状态
        mediaProcess.setStatus("2");
        mediaProcess.setFinishDate(LocalDateTime.now());
        mediaProcess.setUrl(url);
        mediaProcessMapper.updateById(mediaProcess);

        //将mediaprocess表记录插入到mediaprocesshistory表
        MediaProcessHistory mediaProcessHistory = new MediaProcessHistory();
        BeanUtils.copyProperties(mediaProcess,mediaProcessHistory);
        mediaProcessHistoryMapper.insert(mediaProcessHistory);

        //从mediaprocess删除当前任务
        mediaProcessMapper.deleteById(taskId);
    }


}
