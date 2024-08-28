package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.CoursePreviewDto;

import java.io.File;

/**
 * @author CCL
 * @version 1.0
 * @description 课程发布相关的接口
 * @createTime 2024-08-04 20:39
 **/
public interface CoursePublishService {

    /**
     * 获取课程预览信息
     * @param courseId 课程id
     * @return CoursePreviewDto
     */
    public CoursePreviewDto getCoursePreviewInfo(Long courseId);

    /**
     * 提交审核信息
     * @param companyId 机构id
     * @param courseId  课程id
     */
    public void commitAudit(Long companyId, Long courseId);

    /**
     * 课程发布接口
     * @param companyId
     * @param courseId
     */
    public void publish(Long companyId,Long courseId);

    /**
     * 课程页面静态化
     * @param courseId 课程id
     * @return 静态化文件
     */
    public File generateCourseHtml(Long courseId);

    /**
     * 上传课程静态化页面
     * @param courseId 课程id
     * @param file  静态化文件
     */
    public void uploadCourseHtml(Long courseId, File file);

}
