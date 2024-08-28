package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.CommonError;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.mapper.CoursePublishPreMapper;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.*;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
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

    @Autowired
    private CoursePublishMapper coursePublishMapper;

    @Autowired
    private MqMessageService mqMessageService;

    @Autowired
    private MediaServiceClient mediaServiceClient;

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

    @Transactional
    @Override
    public void publish(Long companyId, Long courseId) {

        //查询预发布表写数据
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        //课程状态
        String status = coursePublishPre.getStatus();
        //课程如果没有审核通过 不允许发布
        if (!"202004".equals(status)){
            XueChengPlusException.cast("课程未通过，不允许通过");
        }


        CoursePublish coursePublish = new CoursePublish();
        BeanUtils.copyProperties(coursePublishPre,coursePublish);
        //先查询课程发布，如果有则更新，没有再插入
        CoursePublish coursePublishObj = coursePublishMapper.selectById(courseId);
        if (coursePublishObj == null){
            coursePublishMapper.insert(coursePublish);
        }else {
            coursePublishMapper.updateById(coursePublish);
        }
        //向消息表写数据
//        mqMessageService.addMessage("course_publish", String.valueOf(courseId),null,null);
        saveCoursePublishMessage(courseId);

        //将预发布表数据删除
        coursePublishPreMapper.deleteById(courseId);
    }

    @Override
    public File generateCourseHtml(Long courseId) {
        Configuration configuration = new Configuration(Configuration.getVersion());

        //创建返回文件
        File htmlFile = null;

        try {
            //拿到classpath路径
            String classPath = this.getClass().getResource("/").getPath();
            //指定模板目录
            configuration.setDirectoryForTemplateLoading(new File(classPath + "/templates/"));
            //指定编码
            configuration.setDefaultEncoding("utf-8");

            //得到模板
            Template courseTemplate = configuration.getTemplate("course_template.ftl");

            //准备数据
            CoursePreviewDto coursePreviewInfo = this.getCoursePreviewInfo(courseId);

            HashMap<String, Object> map = new HashMap<>();
            map.put("model",coursePreviewInfo);

            //Template template 模板，Object model 数据
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(courseTemplate, map);

            //输入流
            InputStream inputStream = IOUtils.toInputStream(html, "utf-8");

            htmlFile = File.createTempFile("coursepublish", ".html");

            //输出文件
            FileOutputStream outputStream = new FileOutputStream(htmlFile);

            //使用流 将html写入文件
            IOUtils.copy(inputStream,outputStream);
        } catch (Exception ex) {
            log.error("页面静态化异常，课程id：{}",courseId,ex);
            ex.printStackTrace();
        }
        return htmlFile;
    }

    @Override
    public void uploadCourseHtml(Long courseId, File file) {
        try {
            //将file转成MultipartFile
            MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);

            String upload = mediaServiceClient.upload(multipartFile, "course/121.html");
            if (upload == null){
                log.debug("无可用媒资服务，走降级逻辑得到上传结果为null，课程id：{}",courseId);
                XueChengPlusException.cast("上传静态文件过程中存在异常");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            XueChengPlusException.cast("上传静态文件过程中存在异常");
        }
    }

    /**
     * 保存消息表记录
     * @param courseId
     */
    private void saveCoursePublishMessage(Long courseId){
        MqMessage mqMessage = mqMessageService.addMessage("course_publish", String.valueOf(courseId), null, null);
        if (mqMessage == null){
            XueChengPlusException.cast(CommonError.UNKOWN_ERROR);
        }
    }

}
