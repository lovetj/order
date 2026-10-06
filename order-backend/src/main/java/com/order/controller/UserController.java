package com.order.controller;

import com.order.common.Result;
import com.order.dto.PhoneLoginDTO;
import com.order.dto.WxLoginDTO;
import com.order.entity.User;
import com.order.service.UserService;
import com.order.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 顾客端 —— 登录 / 用户信息
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 微信小程序登录（openid 登录，保留兼容）
     *
     * 前端 wx.login 取 code 后调用本接口换取 token。
     * 首次登录会自动建档，不提供注册接口。
     */
    @PostMapping("/wx-login")
    public Result<Map<String, Object>> wxLogin(@RequestBody WxLoginDTO dto) {
        return Result.success(userService.wxLogin(dto));
    }

    /**
     * 顾客手机号登录（当前顾客端主登录方式）
     *
     * 小程序端：getPhoneNumber 授权 code（phoneCode）由后端换取真实手机号；
     * H5 端：直接提交 phone。
     * 按手机号查用户，不存在则自动建档，返回 { token, user, isNewUser }。
     */
    @PostMapping("/phone-login")
    public Result<Map<String, Object>> phoneLogin(@RequestBody PhoneLoginDTO dto) {
        return Result.success(userService.phoneLogin(dto));
    }

    /** 当前用户信息 */
    @GetMapping("/info")
    public Result<User> info(@RequestHeader(value = "Authorization", required = false) String authorization,
                             @RequestHeader(value = "userId", required = false) String headerUserId) {
        String userId = resolveUserId(authorization, headerUserId);
        if (userId == null) {
            return Result.error(401, "用户未登录，请先登录");
        }
        return Result.success(userService.getById(userId));
    }

    /**
     * 更新用户资料（昵称/头像）
     * 前端以 JSON body 提交：{ nickname, avatar }
     */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody(required = false) Map<String, String> body,
                                      @RequestHeader(value = "Authorization", required = false) String authorization,
                                      @RequestHeader(value = "userId", required = false) String headerUserId) {
        String userId = resolveUserId(authorization, headerUserId);
        if (userId == null) {
            return Result.error(401, "用户未登录，请先登录");
        }
        Map<String, String> params = body == null ? new java.util.HashMap<>() : body;
        userService.updateProfile(userId, params.get("nickname"), params.get("avatar"));
        return Result.success();
    }

    private String resolveUserId(String authorization, String headerUserId) {
        if (authorization != null && !authorization.trim().isEmpty() && jwtUtil.validateToken(authorization)) {
            return jwtUtil.getUserId(authorization);
        }
        if (headerUserId != null && !headerUserId.trim().isEmpty()) {
            return headerUserId.trim();
        }
        return null;
    }
}