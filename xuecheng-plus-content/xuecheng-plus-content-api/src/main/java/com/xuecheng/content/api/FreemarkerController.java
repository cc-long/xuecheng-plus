package com.xuecheng.content.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author CCL
 * @version 1.0
 * @description Freemarker 入门程序
 * @createTime 2024-07-31 21:22
 **/
@Controller
public class FreemarkerController {

    @GetMapping("/testfreemarker")
    public ModelAndView test(){

        ModelAndView modelAndView = new ModelAndView();
        //指定了模型
        modelAndView.addObject("name","小明");
        //指定模板
        //根据试图名称 + .ftl 找到模板
        modelAndView.setViewName("test");
        return modelAndView;
    }
}
