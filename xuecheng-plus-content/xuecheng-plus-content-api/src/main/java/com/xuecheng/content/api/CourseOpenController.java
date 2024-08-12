package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.service.CoursePublishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author CCL
 * @version 1.0
 * @description TODO
 * @createTime 2024-08-04 21:48
 **/

@RestController
@RequestMapping("/open")
public class CourseOpenController {

    @Autowired
    private CoursePublishService coursePublishService;

    //根据课程的id查询课程的信息
    @GetMapping("/course/whole/{courseId}")
    public CoursePreviewDto getPreviewInfo(@PathVariable("courseId") Long courseId){
        //获取课程预览信息
        return coursePublishService.getCoursePreviewInfo(courseId);
    }

}
