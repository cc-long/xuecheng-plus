package com.xuecheng.ucenter.service;

import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;

/**
 * @author CCL
 * @version 1.0
 * @description 统一的认证接口
 * @createTime 2024-09-03 22:13
 **/
public interface AuthService {

    /**
     * 认证方法
     * @param authParamsDto 认证参数
     * @return XcUser 用户信息
     */
    XcUserExt execute(AuthParamsDto authParamsDto);
}
