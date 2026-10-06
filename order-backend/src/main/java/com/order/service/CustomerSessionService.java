package com.order.service;

/**
 * 顾客登录态 Redis 会话服务（三要素：user + shop + table）
 *
 * 登录字符串(token) 存于 Redis，key = login:{userId}:{shopId}:{tableId}，
 * TTL 由 customer.session.expire-seconds 配置（默认 1 天）。
 */
public interface CustomerSessionService {

    /**
     * 绑定/刷新三要素登录态：将 token 写入 Redis 并设置过期时间
     *
     * @return 绑定成功返回 token；三要素不完整返回 null
     */
    String bindLogin(String userId, String shopId, String tableId, String token);

    /**
     * 校验三要素登录态：key 存在且 value 与 token 一致
     */
    boolean validateLogin(String userId, String shopId, String tableId, String token);

    /** 是否已建立三要素登录态（key 存在） */
    boolean isBound(String userId, String shopId, String tableId);

    /** 三要素是否完整（均非空） */
    boolean isEligible(String userId, String shopId, String tableId);

    /** 移除三要素登录态（换桌/退出用） */
    void removeLogin(String userId, String shopId, String tableId);

    /** 登录态有效期（秒），来自配置 */
    long getExpireSeconds();
}