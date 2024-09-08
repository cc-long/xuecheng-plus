package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.mapper.XcUserRoleMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.model.po.XcUserRole;
import com.xuecheng.ucenter.service.AuthService;
import com.xuecheng.ucenter.service.WxAuthService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import springfox.documentation.spring.web.json.Json;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * @author CCL
 * @version 1.0
 * @description 微信扫码认证
 * @createTime 2024-09-03 22:16
 **/
@Service("wx_authservice")
public class WxAuthServiceImpl implements AuthService, WxAuthService {

    //代理当前方法 调用事务方法，事务方法才可生效
    @Autowired
    private WxAuthServiceImpl currentProxy;

    @Autowired
    private XcUserMapper xcUserMapper;

    @Autowired
    private XcUserRoleMapper xcUserRoleMapper;

    @Value("${weixin.appid}")
    private String appid;

    @Value("${weixin.secret}")
    private String secret;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {

        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<>(XcUser.class).eq(XcUser::getUsername, authParamsDto.getUsername()));

        if (xcUser == null) {
            throw new RuntimeException("用户不存在");
        }
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser, xcUserExt);
        return xcUserExt;
    }

    @Override
    public XcUser wxAuth(String code) {
        //申请令牌
        Map<String, String> accessTokenMap = getAccess_token(code);

        // 携带令牌查询用户信息
        //获取参数
        String accessToken = accessTokenMap.get("access_token");
        String openid = accessTokenMap.get("openid");

        Map<String, String> userInfo = getUserInfo(accessToken, openid);

        // 保存用户的信息到数据库
        XcUser xcUser = currentProxy.addWxUser(userInfo);
        return xcUser;
    }

    /**
     * 携带授权码申请令牌
     * https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code
     * @param code 授权码
     * @return
     * {
     * "access_token":"ACCESS_TOKEN",
     * "expires_in":7200,
     * "refresh_token":"REFRESH_TOKEN",
     * "openid":"OPENID",
     * "scope":"SCOPE",
     * "unionid": "o6_bmasdasdsad6_2sgVt7hMZOPfL"
     * }
     */
    private Map<String,String> getAccess_token(String code){

        //请求路径
        String url_template = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";
        String url = String.format(url_template, appid, secret, code);

        //远程调用此url
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, null, String.class);
        //获取响应的结果
        String result = exchange.getBody();
        //将响应结果转换为map
        Map<String, String> map = JSON.parseObject(result, Map.class);

        return map;
    }


    /**
     * 携带令牌查询用户信息
     * https://api.weixin.qq.com/sns/userinfo?access_token=ACCESS_TOKEN&openid=OPENID
     * @param accessToken 令牌
     * @param openid
     * @return
     * {
     * "openid":"OPENID",
     * "nickname":"NICKNAME",
     * "sex":1,
     * "province":"PROVINCE",
     * "city":"CITY",
     * "country":"COUNTRY",
     * "headimgurl": "https://thirdwx.qlogo.cn/mmopen/g3MonUZtNHkdmzicIlibx6iaFqAc56vxLSUfpb6n5WKSYVY0ChQKkiaJSgQ1dZuTOgvLLrhJbERQQ4eMsv84eavHiaiceqxibJxCfHe/0",
     * "privilege":[
     * "PRIVILEGE1",
     * "PRIVILEGE2"
     * ],
     * "unionid": " o6_bmasdasdsad6_2sgVt7hMZOPfL"
     *
     * }
     */
    private Map<String,String> getUserInfo(String accessToken, String openid){

        String urlTemplate = "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s";
        String url = String.format(urlTemplate, accessToken, openid);

        //远程调用此url
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
        //获取响应的结果
        String result = new String(exchange.getBody().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        //将响应结果转换为map
        Map<String, String> map = JSON.parseObject(result, Map.class);

        return map;
    }

    @Transactional
    public XcUser addWxUser(Map<String, String> userInfo) {
        String unionid = userInfo.get("unionid");//微信unionId
        String nickname = userInfo.get("nickname");//微信昵称
        String userPic = userInfo.get("headimgurl");
        //根据unionId 查询用户信息
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<>(XcUser.class).eq(XcUser::getWxUnionid, unionid));

        if (xcUser != null){
            return  xcUser;
        }

        //向数据库新增记录
        xcUser = new XcUser();
        xcUser.setId(UUID.randomUUID().toString());//主键
        xcUser.setUsername(unionid);//用户名
        xcUser.setPassword(unionid);//密码
        xcUser.setWxUnionid(unionid);//微信unionid
        xcUser.setNickname(nickname);//昵称
        xcUser.setUserpic(userPic);
        xcUser.setName(nickname);
        xcUser.setUtype("101001");//学生类型
        xcUser.setStatus("1");//用户状态
        xcUser.setCreateTime(LocalDateTime.now());

        xcUserMapper.insert(xcUser);

        //向用户角色关系表新增记录
        XcUserRole xcUserRole = new XcUserRole();
        xcUserRole.setId(UUID.randomUUID().toString());
        xcUserRole.setUserId(xcUser.getId());
        xcUserRole.setRoleId("17");//学生角色id
        xcUserRole.setCreateTime(LocalDateTime.now());
        xcUserRoleMapper.insert(xcUserRole);
        return xcUser;
    }

}
