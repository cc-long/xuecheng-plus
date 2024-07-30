package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachPlanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    @Autowired
    private TeachplanMediaMapper teachplanMediaMapper;
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

    @Override
    public void deleteCourseBase(Long teachplanId) {
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        if (teachplan.getParentid() == 0 ){
            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Teachplan::getParentid,teachplanId);
            List<Teachplan> teachplans = teachplanMapper.selectList(queryWrapper);
            if (teachplans.isEmpty()){
                teachplanMapper.deleteById(teachplanId);
            }else {
                XueChengPlusException.cast("课程计划信息还有子级信息，无法操作");
            }
        }else {
            teachplanMapper.deleteById(teachplanId);
            LambdaQueryWrapper<TeachplanMedia> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TeachplanMedia::getTeachplanId,teachplanId);
            teachplanMediaMapper.delete(queryWrapper);
        }
    }

    @Override
    @Transactional
    public TeachplanMedia associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto) {

        Teachplan teachplan = teachplanMapper.selectById(bindTeachplanMediaDto.getTeachplanId());
        if (teachplan == null){
            XueChengPlusException.cast("课程计划不存在");
        }
        if (teachplan.getGrade() != 2){
            XueChengPlusException.cast("只允许第二级教学计划绑定媒资文件");
        }
        //先删除原有记录，根据课程计划的id 删除它所绑定的媒资
        int delete = teachplanMediaMapper.delete(new LambdaQueryWrapper<TeachplanMedia>()
                .eq(TeachplanMedia::getTeachplanId, bindTeachplanMediaDto.getTeachplanId())
        );

        //再添加新记录
        TeachplanMedia teachplanMedia = new TeachplanMedia();
        teachplanMedia.setCourseId(teachplan.getCourseId());
        BeanUtils.copyProperties(bindTeachplanMediaDto,teachplanMedia);
        teachplanMedia.setMediaFilename(bindTeachplanMediaDto.getFileName());
        teachplanMedia.setCreateDate(LocalDateTime.now());
        teachplanMediaMapper.insert(teachplanMedia);
        return teachplanMedia;
    }
}
