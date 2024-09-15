package com.xuecheng.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author CCL
 * @version 1.0
 * @description TODO
 * @createTime 2024-09-11 22:01
 **/
@Slf4j
@Service
public class MyCourseTableServiceImpl implements MyCourseTableService {

    @Autowired
    private XcChooseCourseMapper xcChooseCourseMapper;

    @Autowired
    private XcCourseTablesMapper xcCourseTablesMapper;

    @Autowired
    private ContentServiceClient contentServiceClient;

    @Transactional
    @Override
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId) {

        //选课调用内容管理查询课程的收费规则
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if (coursepublish == null){
            XueChengPlusException.cast("课程不存在");
        }
        //收费规则
        String charge = coursepublish.getCharge();
        //选课记录
        XcChooseCourse chooseCourse = null;
        if ("201000".equals(charge)){
            //免费课程  会向选课记录表
            chooseCourse = addFreeCourse(userId, coursepublish);
            //向我的课程表写
            XcCourseTables xcCourseTables = addCourseTables(chooseCourse);

        }else {
            //收费课程 会向选课记录表写数据
            chooseCourse = addChargeCourse(userId, coursepublish);
            //支付订单
            //向我的课程表写数据
        }

        //判断学生的学习资格
        XcCourseTablesDto learningStatus = getLearningStatus(userId, courseId);
        //构造返回值
        XcChooseCourseDto xcChooseCourseDto = new XcChooseCourseDto();
        BeanUtils.copyProperties(chooseCourse,xcChooseCourseDto);
        //设置学习资格状态
        xcChooseCourseDto.setLearnStatus(learningStatus.getLearnStatus());
        return xcChooseCourseDto;
    }

    @Override
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId) {
        //返回的结果
        XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
        //查询我的课程表，如果查不到 说明没有选课
        XcCourseTables xcCourseTables = getXcCourseTables(userId, courseId);
        if (xcCourseTables == null){
            //{"code":"702002","desc":"没有选课或选课后没有支付"}
            BeanUtils.copyProperties(xcCourseTables,xcCourseTablesDto);
            xcCourseTablesDto.setLearnStatus("702002");
            return xcCourseTablesDto;
        }

        //如果查到了，判断是否过期。过期不能学习
        //[{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
        boolean before = xcCourseTables.getValidtimeEnd().isBefore(LocalDateTime.now());
        if (before){
            //{"code":"702003","desc":"已过期需要申请续期或重新支付"}
            BeanUtils.copyProperties(xcCourseTables,xcCourseTablesDto);
            xcCourseTablesDto.setLearnStatus("702003");
            return xcCourseTablesDto;
        }else {
            //{"code":"702001","desc":"正常学习"}
            BeanUtils.copyProperties(xcCourseTables,xcCourseTablesDto);
            xcCourseTablesDto.setLearnStatus("702001");
            return xcCourseTablesDto;
        }
    }

    //添加免费课程，免费课程加入选课记录表，我的课程表
    private XcChooseCourse addFreeCourse(String userId, CoursePublish coursepublish) {
        //课程id
        Long courseId = coursepublish.getId();
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<XcChooseCourse>()
                .eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, courseId)
                .eq(XcChooseCourse::getOrderType, "700001")//免费课程
                .eq(XcChooseCourse::getStatus, "701001");//选课成功
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if (xcChooseCourses.size() > 0){
            return xcChooseCourses.get(0);
        }

        //向选课记录表写数据
        XcChooseCourse xcChooseCourse = new XcChooseCourse();

        xcChooseCourse.setCourseId(courseId);
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700001");
        xcChooseCourse.setStatus("701001");
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setCoursePrice(coursepublish.getPrice());
        xcChooseCourse.setValidDays(365);
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));
        int insert = xcChooseCourseMapper.insert(xcChooseCourse);
        if (insert <= 0){
            XueChengPlusException.cast("添加选课记录失败");
        }
        return xcChooseCourse;
    }

    //添加到我的课程表
    public XcCourseTables addCourseTables(XcChooseCourse xcChooseCourse){

        //选课成功了才可以向我的课程表添加
        String status = xcChooseCourse.getStatus();
        if (!"701001".equals(status)){
            XueChengPlusException.cast("选课不成功，无法添加到课程表");
        }
        XcCourseTables xcCourseTables = getXcCourseTables(xcChooseCourse.getUserId(), xcChooseCourse.getCourseId());
        if (xcCourseTables != null){
            return xcCourseTables;
        }
        xcCourseTables = new XcCourseTables();
        BeanUtils.copyProperties(xcChooseCourse,xcCourseTables);
        xcCourseTables.setChooseCourseId(xcChooseCourse.getId());//记录课程表的主键
        xcCourseTables.setCourseType(xcChooseCourse.getOrderType());//选课类型
        xcCourseTables.setUpdateDate(LocalDateTime.now());
        int insert = xcCourseTablesMapper.insert(xcCourseTables);
        if (insert <= 0){
            XueChengPlusException.cast("添加我的课程表失败");
        }
        return xcCourseTables;
    }

    //根据用户id  和课程id 查询选课表
    public XcCourseTables getXcCourseTables(String userId, Long courseId) {
        return xcCourseTablesMapper.selectOne(new LambdaQueryWrapper<XcCourseTables>()
                .eq(XcCourseTables::getUserId, userId)
                .eq(XcCourseTables::getCourseId, courseId)
        );
    }

    //添加收费课程
    public XcChooseCourse addChargeCourse(String userId, CoursePublish coursepublish){
        //课程id
        Long courseId = coursepublish.getId();
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<XcChooseCourse>()
                .eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, courseId)
                .eq(XcChooseCourse::getOrderType, "700002")//收费课程
                .eq(XcChooseCourse::getStatus, "701002");//待支付
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if (xcChooseCourses.size() > 0){
            return xcChooseCourses.get(0);
        }

        //向选课记录表写数据
        XcChooseCourse xcChooseCourse = new XcChooseCourse();

        xcChooseCourse.setCourseId(courseId);
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700002");
        xcChooseCourse.setStatus("701002");
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setCoursePrice(coursepublish.getPrice());
        xcChooseCourse.setValidDays(365);
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));
        int insert = xcChooseCourseMapper.insert(xcChooseCourse);
        if (insert <= 0){
            XueChengPlusException.cast("添加选课记录失败");
        }
        return xcChooseCourse;
    }

}
