package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import lombok.Data;

import java.util.List;

/**
 * @author CCL
 * @version 1.0
 * @description TODO
 * @createTime 2024-07-07 21:50
 **/
@Data
public class TeachplanDto  extends Teachplan {

    //与媒资管理的信息

    private TeachplanMedia teachplanMedia;

    //小章节List
    private List<TeachplanDto> teachPlanTreeNodes;
}
