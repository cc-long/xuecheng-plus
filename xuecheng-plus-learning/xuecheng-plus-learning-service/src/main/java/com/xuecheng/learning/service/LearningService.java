package com.xuecheng.learning.service;

import com.xuecheng.base.model.RestResponse;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author CCL
 * @version 1.0
 * @description 在线学习相关接口
 * @createTime 2024-09-27 02:32
 **/
public interface LearningService {

    /**
     * 获取视频
     * @param userId 用户id
     * @param courseId 课程id
     * @param teachplanId 教学章节id
     * @param mediaId 视频id
     * @return RestResponse<String>
     */
    public RestResponse<String> getVideo(String userId, Long courseId, Long teachplanId, String mediaId);
}
