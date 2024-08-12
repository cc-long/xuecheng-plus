package com.xuecheng.content.model.dto;

import lombok.Data;

import java.util.List;

/**
 * @author CCL
 * @version 1.0
 * @description 课程预览模型类
 * @createTime 2024-08-04 20:34
 **/
@Data
public class CoursePreviewDto {

    //课程的基本信息、营销信息
    private CourseBaseInfoDto courseBase;

    //课程计划信息
    private List<TeachplanDto> teachplans;

    //课程的师资信息 TODO
}
