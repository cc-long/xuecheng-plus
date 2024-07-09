package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.model.dto.SaveTeachPlanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.service.TeachplanService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 课程计划 服务实现类
 * </p>
 *
 * @author CCL
 * @since 2024-07-07
 */
@Service
public class TeachplanServiceImpl extends ServiceImpl<TeachplanMapper, Teachplan> implements TeachplanService {

    @Autowired
    private TeachplanMapper teachplanMapper;
    @Override
    public List<TeachplanDto> findTeachPlanTree(Long courseId) {
        return teachplanMapper.selectTreeNodes(courseId);
    }

    //新增 修改 保存 课程计划
    @Override
    public void saveTeachPlan(SaveTeachPlanDto saveTeachPlanDto) {

        Long id = saveTeachPlanDto.getId();
        if (id == null){
            //新增
            Teachplan teachplan = new Teachplan();
            BeanUtils.copyProperties(saveTeachPlanDto,teachplan);
            //确定排序字段,找到它的同级节点个数，排序字段就是个数+1 select count(1) from teachplan where course_id=117 and paretid=268
            //select max(orderby) from teachplan where course_id=117 and parentid=268;
            Long courseId = saveTeachPlanDto.getCourseId();
            Long parentId = saveTeachPlanDto.getParentid();
//            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
//            queryWrapper.eq(Teachplan::getCourseId,courseId);
//            queryWrapper.eq(Teachplan::getParentid,parentId);
            Integer courseCount = teachplanMapper.selectMax(courseId,parentId);
            if (courseCount == null) courseCount = 0;

            teachplan.setOrderby(courseCount+1);

            teachplanMapper.insert(teachplan);
        }else {
            //修改
            Teachplan teachplan = teachplanMapper.selectById(id);
            BeanUtils.copyProperties(saveTeachPlanDto,teachplan);
            teachplanMapper.updateById(teachplan);
        }
    }
}
