package com.order.dto;

import lombok.Data;

/**
 * 微信小程序登录 DTO
 */
@Data
public class WxLoginDTO {

    /** wx.login 返回的 code */
    private String code;

    /** 微信昵称（授权返回） */
    private String nickname;

    /** 微信头像（授权返回） */
    private String avatar;

    /** 角色 customer / merchant，默认 customer */
    private String role;
}
