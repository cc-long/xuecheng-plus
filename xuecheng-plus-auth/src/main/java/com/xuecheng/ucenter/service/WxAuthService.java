package com.xuecheng.ucenter.service;

import com.xuecheng.ucenter.model.po.XcUser;

/**
 * @author CCL
 * @version 1.0
 * @description 微信扫码接入
 * @createTime 2024-09-07 00:15
 **/
public interface WxAuthService {

    /**
     * 微信扫码认证：1。申请令牌
     *              2.携带令牌查询用户信息
     *              3. 保存用户的信息到数据库
     * @param code 授权码
     * @return 用户信息
     */
    public XcUser wxAuth(String code);
}
