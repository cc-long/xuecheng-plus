package com.xuecheng.learning.service;

import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;

/**
 * @author CCL
 * @version 1.0
 * @description TODO
 * @createTime 2024-09-11 22:01
 **/
public interface MyCourseTableService {

    /**
     * 添加选课
     * @param userId 用户id
     * @param courseId 课程id
     * @return 学生选课信息
     */
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId);

    /**
     * 判断学习资格
     * @param userId
     * @param courseId
     * @return 学习资格状态
     * [{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
     */
    public XcCourseTablesDto getLearningStatus(String userId,Long courseId);

    /**
     *保存选课成功状态
     * @param chooseCourseId 选课表id
     * @return
     */
    public boolean saveChooseCourseSuccess(String chooseCourseId);
}
