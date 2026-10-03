package com.order.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.order.config.WechatProperties;
import com.order.dto.LoginDTO;
import com.order.dto.WxLoginDTO;
import com.order.entity.User;
import com.order.mapper.UserMapper;
import com.order.service.UserService;
import com.order.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String WX_LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private WechatProperties wechatProperties;

    @Override
    public String login(LoginDTO dto) {
        // 顾客端不提供账号密码登录（仅允许微信登录），此方法保留供内部/测试使用
        User user = getByUsername(dto.getUsername());
        if (user == null || !StringUtils.hasText(user.getPassword())
                || !BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new RuntimeException("账号已被禁用");
        }
        return jwtUtil.generateToken(user.getId(), user.getUsername(), "customer");
    }

    /**
     * 微信小程序登录（参考 mall 项目实现）
     *
     * 流程：wx.login 取 code -> 调用微信 jscode2session 换 openid/unionid/session_key
     *      -> 按 openid 查用户，不存在则自动注册（不提供注册入口，属于首次登录静默建档）
     *      -> 生成 JWT，返回 { token, user, isNewUser }
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> wxLogin(WxLoginDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getCode())) {
            throw new RuntimeException("微信授权凭证 code 不能为空");
        }

        WechatProperties.Miniapp miniapp = wechatProperties.getMiniapp();
        String openid = null;
        String unionid = null;
        String sessionKey = null;

        boolean hasAppId = StringUtils.hasText(miniapp.getAppId());
        boolean hasSecret = StringUtils.hasText(miniapp.getSecret());

        if (hasAppId && hasSecret) {
            // 真实模式：调用微信官方 jscode2session
            try {
                String url = WX_LOGIN_URL + "?appid=" + miniapp.getAppId().trim()
                        + "&secret=" + miniapp.getSecret().trim()
                        + "&js_code=" + dto.getCode().trim()
                        + "&grant_type=authorization_code";
                String resp = HttpUtil.get(url, 5000);
                JSONObject json = JSON.parseObject(resp);
                if (json == null) {
                    throw new RuntimeException("微信登录失败：未收到微信返回");
                }
                Integer errcode = json.getInteger("errcode");
                if (errcode != null && errcode != 0) {
                    log.warn("[微信登录] 微信接口返回错误 errcode={}, errmsg={}",
                            errcode, json.getString("errmsg"));
                    throw new RuntimeException("微信登录失败：" + json.getString("errmsg"));
                }
                openid = json.getString("openid");
                unionid = json.getString("unionid");
                sessionKey = json.getString("session_key");
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                log.warn("[微信登录] 请求微信接口异常: {}", e.getMessage());
                throw new RuntimeException("请求微信服务失败：" + e.getMessage());
            }
        } else {
            // Mock 模式：未配置 appId/secret 时本地联调使用
            if (!miniapp.isMock()) {
                throw new RuntimeException("微信小程序配置未就绪，请联系管理员");
            }
            openid = "mock_wx_openid_" + cn.hutool.crypto.digest.DigestUtil.md5Hex(dto.getCode()).substring(0, 16);
            log.warn("[微信登录] 使用 Mock 模式生成 openid={}", openid);
        }

        if (!StringUtils.hasText(openid)) {
            throw new RuntimeException("微信登录失败：未获取到 openid");
        }

        // 按 openid 查询用户，不存在则自动建档（首次微信登录）
        User user = getByOpenid(openid);
        boolean isNewUser = false;
        if (user == null) {
            isNewUser = true;
            user = new User();
            user.setOpenid(openid);
            user.setUnionid(unionid);
            user.setSessionKey(sessionKey);
            user.setUsername(generateUsername(openid));
            user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname().trim() : "微信用户");
            user.setAvatar(StringUtils.hasText(dto.getAvatar()) ? dto.getAvatar().trim() : null);
            user.setMemberLevel("普通会员");
            user.setPoints(0);
            user.setStatus(1);
            save(user);
        } else {
            if (user.getStatus() != null && user.getStatus() != 1) {
                throw new RuntimeException("账号已被禁用");
            }
            boolean changed = false;
            // session_key 每次登录都会刷新，直接覆盖
            if (StringUtils.hasText(sessionKey) && !sessionKey.equals(user.getSessionKey())) {
                user.setSessionKey(sessionKey);
                changed = true;
            }
            // unionid 仅在首次获取到时补全
            if (StringUtils.hasText(unionid) && !StringUtils.hasText(user.getUnionid())) {
                user.setUnionid(unionid);
                changed = true;
            }
            if (StringUtils.hasText(dto.getNickname()) && !dto.getNickname().trim().equals(user.getNickname())) {
                user.setNickname(dto.getNickname().trim());
                changed = true;
            }
            if (StringUtils.hasText(dto.getAvatar()) && !dto.getAvatar().trim().equals(user.getAvatar())) {
                user.setAvatar(dto.getAvatar().trim());
                changed = true;
            }
            if (changed) {
                updateById(user);
            }
        }

        String role = StringUtils.hasText(dto.getRole()) ? dto.getRole() : "customer";
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), role);

        // 不下发敏感字段
        user.setPassword(null);
        user.setSessionKey(null);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        result.put("isNewUser", isNewUser);
        return result;
    }

    /**
     * 生成安全的用户名：wx_ + openid 摘要，长度可控且唯一
     */
    private String generateUsername(String openid) {
        String digest = cn.hutool.crypto.digest.DigestUtil.md5Hex(openid);
        return "wx_" + digest;
    }

    @Override
    public User getByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username.trim()));
    }

    @Override
    public User getByOpenid(String openid) {
        if (!StringUtils.hasText(openid)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<User>().eq(User::getOpenid, openid.trim()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(String userId, String nickname, String avatar) {
        User user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (StringUtils.hasText(nickname)) {
            user.setNickname(nickname);
        }
        if (StringUtils.hasText(avatar)) {
            user.setAvatar(avatar);
        }
        updateById(user);
    }
}
