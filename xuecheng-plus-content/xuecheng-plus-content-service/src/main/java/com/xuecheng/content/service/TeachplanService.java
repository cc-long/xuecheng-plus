package com.xuecheng.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachPlanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * <p>
 * 课程计划 服务类
 * </p>
 *
 * @author CCL
 * @since 2024-07-07
 */
public interface TeachplanService extends IService<Teachplan> {

    /**
     * 根据课程ID查询课程计划
     * @param courseId
     * @return
     */
    public List<TeachplanDto> findTeachPlanTree(Long courseId);

    public void saveTeachPlan(SaveTeachPlanDto saveTeachPlanDto);

    void deleteCourseBase(Long teachplanId);

    /**
     * 教学计划绑定媒资
     * @param bindTeachplanMediaDto
     * @return TeachplanMedia
     */
    TeachplanMedia associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto);
}
