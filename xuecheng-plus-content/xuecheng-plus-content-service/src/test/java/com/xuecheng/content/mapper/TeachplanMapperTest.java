package com.xuecheng.content.mapper;

import com.xuecheng.content.model.dto.TeachplanDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author CCL
 * @version 1.0
 * @description 课程计划mapper 测试
 * @createTime 2024-07-07 23:47
 **/
@SpringBootTest
public class TeachplanMapperTest {

    @Autowired
    TeachplanMapper teachplanMapper;


    @Test
    public void testSelectTreeNodes() {
        List<TeachplanDto> teachplanDtos = teachplanMapper.selectTreeNodes(117L);
        System.out.println(teachplanDtos);
    }

    @Test
    void selectMax() {
        Integer i = teachplanMapper.selectMax(117L, 268L);
        System.out.println(i);
    }
}