package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.mapper.CoursePublishPreMapper;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.model.po.CoursePublishPre;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.TeachplanService;
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
 * @description 课程发布相关接口实现
 * @createTime 2024-08-04 20:42
 **/

@Slf4j
@Service
public class CoursePublishServiceImpl implements CoursePublishService {

    @Autowired
    private CourseBaseInfoService courseBaseInfoService;

    @Autowired
    private TeachplanService teachplanService;

    @Autowired
    private CourseMarketMapper courseMarketMapper;

    @Autowired
    private CoursePublishPreMapper coursePublishPreMapper;

    @Autowired
    private CourseBaseMapper courseBaseMapper;

    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {

        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        //课程基本信息 和营销信息
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        coursePreviewDto.setCourseBase(courseBaseInfo);
        //课程计划
        List<TeachplanDto> teachplan = teachplanService.findTeachPlanTree(courseId);
        coursePreviewDto.setTeachplans(teachplan);
        return coursePreviewDto;
    }

    @Transactional
    @Override
    public void commitAudit(Long companyId, Long courseId) {

        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        if (courseBaseInfo == null){
            XueChengPlusException.cast("课程找不到");
        }
        //审核状态
        String auditStatus = courseBaseInfo.getAuditStatus();
        if ("202003".equals(auditStatus)){
            XueChengPlusException.cast("课程已提交，请等待审核");
        }

        //课程的图片、计划信息没有填写也没有填写
        String pic = courseBaseInfo.getPic();
        if (pic.isEmpty()){
            XueChengPlusException.cast("请上传课程图片");
        }

        //查询课程计划
        List<TeachplanDto> teachPlanTree = teachplanService.findTeachPlanTree(courseId);
        if (teachPlanTree.isEmpty()){
            XueChengPlusException.cast("请编写课程计划");
        }

        //本机构只能提交本机构的课程
        if (!companyId.equals(courseBaseInfo.getCompanyId())){
            XueChengPlusException.cast("本机构只能添加本机构的id");
        }

        //查询到课程基本信息、营销信息、计划信息 插入到课程预发布表
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        BeanUtils.copyProperties(courseBaseInfo,coursePublishPre);

        //设置机构的id
        coursePublishPre.setCompanyId(companyId);

        //营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        String marketJson = JSON.toJSONString(courseMarket);
        coursePublishPre.setMarket(marketJson);

        //计划信息
        String teachPlanJson = JSON.toJSONString(teachPlanTree);
        coursePublishPre.setTeachplan(teachPlanJson);

        //TODO: 师资表查询、合法性校验、封装师资数据

        //状态变为已提交
        coursePublishPre.setStatus("202003");
        //提交时间
        coursePublishPre.setCreateDate(LocalDateTime.now());

        //查询预发布表，如果有记录更新，没有插入
        CoursePublishPre coursePublishPreObj = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPreObj == null)
            coursePublishPreMapper.insert(coursePublishPre);
        else coursePublishPreMapper.updateById(coursePublishPre);

        //更新课程基本信息表的审核状态 为已提交
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        //审核状态 为已提交
        courseBase.setAuditStatus("202003");
        courseBaseMapper.updateById(courseBase);
    }


}
