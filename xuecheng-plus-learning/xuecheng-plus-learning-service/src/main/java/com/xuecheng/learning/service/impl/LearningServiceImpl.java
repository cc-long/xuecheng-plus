package com.xuecheng.learning.service.impl;

import com.xuecheng.base.model.RestResponse;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.feignclient.MediaServiceClient;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.service.LearningService;
import com.xuecheng.learning.service.MyCourseTableService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author CCL
 * @version 1.0
 * @description TODO
 * @createTime 2024-09-27 02:32
 **/
@Service
public class LearningServiceImpl implements LearningService {

    @Autowired
    private ContentServiceClient contentServiceClient;

    @Autowired
    private MyCourseTableService myCourseTableService;

    @Autowired
    private MediaServiceClient mediaServiceClient;

    @Override
    public RestResponse<String> getVideo(String userId, Long courseId, Long teachplanId, String mediaId) {
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if (coursepublish == null){
            return RestResponse.validfail("课程不存在");
        }
        if (StringUtils.isEmpty(userId)){
            //查看课程的收费规则
            if("201000".equals(coursepublish.getCharge())){
                //远程调用 获取视频播放地址
                RestResponse<String> playUrlByMediaId = mediaServiceClient.getPlayUrlByMediaId(mediaId);
                return playUrlByMediaId;
            }else {
                //todo：课程收费，判断试学逻辑
                //todo：试学章节 远程调用内容管理服务 根据课程计划id <teachplanId> 去查询课程计划信息，如果is_preview的值为1 标识支持试学
                return RestResponse.validfail("该课程需要购买");
            }

        }
        //获取学习资格
        XcCourseTablesDto courseTablesDto = myCourseTableService.getLearningStatus(userId, courseId);
        //"[{""code"":""702001"",""desc"":""正常学习""},{""code"":""702002"",""desc"":""没有选课或选课后没有支付""},{""code"":""702003"",""desc"":""已过期需要申请续期或重新支付""}]"
        String learnStatus = courseTablesDto.getLearnStatus();
        if (!"702001".equals(learnStatus)){

            if ("702002".equals(learnStatus)){
                return RestResponse.validfail("没有选课或选课后没有支付");
            }else if ("702003".equals(learnStatus)){
                return RestResponse.validfail("已过期需要申请续期或重新支付");
            }else {
                return RestResponse.validfail("异常课程");
            }
        }

        //远程调用 获取视频播放地址
        RestResponse<String> playUrlByMediaId = mediaServiceClient.getPlayUrlByMediaId(mediaId);
        return playUrlByMediaId;
    }
}
