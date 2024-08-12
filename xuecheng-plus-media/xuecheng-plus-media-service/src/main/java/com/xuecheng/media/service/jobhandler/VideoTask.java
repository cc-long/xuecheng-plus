package com.xuecheng.media.service.jobhandler;

import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.IMediaFilesService;
import com.xuecheng.media.service.IMediaProcessService;
import com.xuecheng.media.utils.Mp4VideoUtil;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 任务处理类
 * @author xuxueli 2019-12-11 21:52:51
 */
@Slf4j
@Component
public class VideoTask {

    @Autowired
    private IMediaProcessService mediaProcessService;

    @Autowired
    private IMediaFilesService mediaFilesService;

    //ffmpeg的路径
    @Value("${videoprocess.ffmpegpath}")
    private String ffmpegPath;

    @XxlJob("videoJobHandler")
    public void videoJobHandler() throws Exception {

        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();


        //确定 几核cpu
        int processors = Runtime.getRuntime().availableProcessors();

        //查询待处理的任务
        List<MediaProcess> mediaProcessList = mediaProcessService.getMediaProcessList(shardTotal, shardIndex, 5);

        //任务数量
        int size = mediaProcessList.size();
        log.debug("取到视频处理任务数：{}",size);
        if (size<=0) return;
        //创建线程池
        ExecutorService executorService = Executors.newFixedThreadPool(size);

        //使用计数器
        CountDownLatch countDownLatch = new CountDownLatch(size);
        mediaProcessList.forEach( mediaProcess -> {
            //将任务加入线程池
            executorService.execute(()->{
                try {
                    //任务的执行逻辑
                    //任务id
                    Long taskId = mediaProcess.getId();
                    //开启任务
                    boolean b = mediaProcessService.startTask(taskId);
                    if (!b){
                        log.debug("抢占任务失败，任务id：{}",taskId);
                        return;
                    }

                    //文件id就是md5
                    String fileId = mediaProcess.getFileId();

                    String bucket = mediaProcess.getBucket();
                    String objectName = mediaProcess.getFilePath();
                    //下载minio视频到本地
                    File file = mediaFilesService.downloadFileFormMinIO(bucket, objectName);
                    if (file == null){
                        log.debug("下载视频出错，任务id：{}，bucket：{}，objectName：{}",taskId,bucket,objectName);
                        //保存任务处理失败的结果
                        mediaProcessService.saveProcessFinishStatus(taskId,"3",fileId,null,"下载视频到本地失败");
                        return;
                    }


                    //源avi视频的路径
                    String videoPath = file.getAbsolutePath();
                    //转码后mp4文件的名称
                    String mp4Name = fileId + ".mp4";
                    File mp4File = null;
                    try {
                        mp4File= File.createTempFile("minio", ".mp4");
                    } catch (IOException e) {
                        log.debug("创建临时文件异常，{}",e.getMessage());
                        //保存任务处理失败的结果
                        mediaProcessService.saveProcessFinishStatus(taskId,"3",fileId,null,"创建临时文件异常");
                        return;
                    }
                    //获取临时文件绝对路径路径
                    String mp4Path = mp4File.getAbsolutePath();
                    String result = null;

                    try {
                        //创建工具类对象
                        Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegPath,videoPath,mp4Name,mp4Path);
                        //开始视频转换，成功将返回success
                        result = videoUtil.generateMp4();
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("处理视频文件:{},出错:{}", mediaProcess.getFilePath(), e.getMessage());
                    }
                    if (!"success".equals(result)){
                        log.debug("视频转码失败，bucket:{},objectName:{}原因：{}",bucket,objectName,result);
                        //保存任务处理失败的结果
                        mediaProcessService.saveProcessFinishStatus(taskId,"3",fileId,null,result);
                        return;
                    }


                    String filePath = getFilePathByMd5(fileId, ".mp4");
                    //上传到minio
                    boolean b1 = mediaFilesService.addMediaFilesToMinIO(mp4Path, "video/mp4", bucket, filePath);
                    if (!b1){
                        log.debug("上传mp4到minio失败，taskId：{}",taskId);
                        //保存任务处理失败的结果
                        mediaProcessService.saveProcessFinishStatus(taskId,"3",fileId,null,result);
                    }



                    //更新任务状态为成功
                    String url = "/" + bucket + "/" + filePath;
                    mediaProcessService.saveProcessFinishStatus(taskId,"2",fileId,url,result);
                } finally {

                    //计数器 减1
                    countDownLatch.countDown();
                }


            });

        });
        //阻塞    实现线程全部结束 方法才结束
        //指定最大限制的等待时间，阻塞最多等待一定的时间后就解除阻塞
        countDownLatch.await(30, TimeUnit.MINUTES);

    }


    //根据文件md5 生成文件路径
    private String getFilePathByMd5(String fileMd5,String fileExt){
        return fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }

}
