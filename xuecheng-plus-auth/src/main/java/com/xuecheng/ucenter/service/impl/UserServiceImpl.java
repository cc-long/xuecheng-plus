package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * @author CCL
 * @version 1.0
 * @description TODO
 * @createTime 2024-09-01 18:06
 **/
@Slf4j
@Component
public class UserServiceImpl implements UserDetailsService {

    @Autowired
    private XcUserMapper xcUserMapper;

    @Autowired
    private ApplicationContext applicationContext;

    //传入的请求认证的参数 就是AuthParamsDto
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        //将传入的json 转成AuthParamsDto对象
        AuthParamsDto authParamsDto = null;

        try {
            authParamsDto = JSON.parseObject(s, AuthParamsDto.class);
        } catch (Exception e) {
            throw new RuntimeException("请求认证参数不符合要求");
        }

        //认证类型，有password、wx
        String authType = authParamsDto.getAuthType();

        //根据认证类型从spring容器取出指定的bean
        String beanName = authType + "_authservice";
        AuthService bean = applicationContext.getBean(beanName, AuthService.class);
        //调用统一execute方法完成认证
        XcUserExt xcUserExt = bean.execute(authParamsDto);
        //封装XcUserExt用户信息 为UserDetails

        return getUserPrincipal(xcUserExt);
    }

    /**
     * 查询用户信息
     * @param xcUserExt 用户信息
     * @return
     */
    public UserDetails getUserPrincipal(XcUserExt xcUserExt){
        String password = xcUserExt.getPassword();
        //权限
        String[] authorities = {"test"};
        xcUserExt.setPassword(null);
        //将用户信息转json
        String userJson = JSON.toJSONString(xcUserExt);
        UserDetails userDetails = User.withUsername(userJson).password(password).authorities(authorities).build();
        return userDetails;
    }
}
