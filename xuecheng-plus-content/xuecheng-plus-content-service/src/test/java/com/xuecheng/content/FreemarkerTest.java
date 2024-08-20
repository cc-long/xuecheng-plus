package com.xuecheng.content;

import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.service.MqMessageService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * @author CCL
 * @version 1.0
 * @description 测试freemarker 页面静态化方法
 * @createTime 2024-08-18 20:27
 **/

@SpringBootTest
public class FreemarkerTest {

    @Autowired
    private CoursePublishService coursePublishService;

    @Test
    public void testGenerateHemlByTemplate() throws IOException, TemplateException {

        Configuration configuration = new Configuration(Configuration.getVersion());

        //拿到classpath路径
        String classPath = this.getClass().getResource("/").getPath();
        //指定模板目录
        configuration.setDirectoryForTemplateLoading(new File(classPath + "/templates/"));
        //指定编码
        configuration.setDefaultEncoding("utf-8");

        //得到模板
        Template courseTemplate = configuration.getTemplate("course_template.ftl");

        //准备数据
        CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(121L);

        HashMap<String, Object> map = new HashMap<>();
        map.put("model",coursePreviewInfo);

        //Template template 模板，Object model 数据
        String html = FreeMarkerTemplateUtils.processTemplateIntoString(courseTemplate, map);

        //输入流
        InputStream inputStream = IOUtils.toInputStream(html, "utf-8");

        //输出文件
        FileOutputStream outputStream = new FileOutputStream(new File("D:\\迅雷下载\\121.html"));

        //使用流 将html写入文件
        IOUtils.copy(inputStream,outputStream);
    }
}
