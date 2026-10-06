package com.order.dto;

import lombok.Data;

/**
 * 顾客手机号登录 DTO
 *
 * 小程序端：wx.getPhoneNumber 授权后拿到 phoneCode，由后端换取真实手机号
 * H5 端：微信授权不可用，直接提交 phone
 */
@Data
public class PhoneLoginDTO {

    /** wx.login 返回的 code（可选，用于关联 openid） */
    private String code;

    /** 微信 getPhoneNumber 授权返回的 code（小程序端必传） */
    private String phoneCode;

    /** H5 端手动输入的手机号（phoneCode 为空时使用） */
    private String phone;

    /** 昵称（可选，作为用户资料同步） */
    private String nickname;

    /** 头像（可选，作为用户资料同步） */
    private String avatar;

    /** 角色 customer / merchant，默认 customer */
    private String role;

    /** 扫码桌位店铺ID（可选，用于登录时绑定三要素登录态） */
    private String shopId;

    /** 扫码桌位ID（可选，用于登录时绑定三要素登录态） */
    private String tableId;
}
