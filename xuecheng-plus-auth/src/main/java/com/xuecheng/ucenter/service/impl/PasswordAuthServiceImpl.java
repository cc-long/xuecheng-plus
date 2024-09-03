package com.xuecheng.ucenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * @author CCL
 * @version 1.0
 * @description 账号密码认证
 * @createTime 2024-09-03 22:16
 **/
@Service("password_authservice")
public class PasswordAuthServiceImpl implements AuthService {

    @Autowired
    private XcUserMapper xcUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        //账号
        String username = authParamsDto.getUsername();

        //TODO:校验验证码

        //根据username账号 查询数据库
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));


        //用户不存在返回null，spring security框架抛出异常 用户不存在
        if(xcUser == null) {
            throw new RuntimeException("用户不存咋");
        }

        //验证密码是否正确
        //如果查到了用户 拿到正确的密码
        String passwordDb = xcUser.getPassword();
        //拿到用户输入密码
        String passwordForm = authParamsDto.getPassword();

        //校验密码
        boolean matches = passwordEncoder.matches(passwordForm, passwordDb);

        if(!matches) {
            throw new RuntimeException("账号或密码错误");
        }

        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser,xcUserExt);
        return xcUserExt;
    }
}
