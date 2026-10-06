package com.order.service.impl;

import com.order.service.CustomerSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Service
public class CustomerSessionServiceImpl implements CustomerSessionService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${customer.session.key-prefix:login:}")
    private String keyPrefix;

    @Value("${customer.session.expire-seconds:86400}")
    private long expireSeconds;

    @Override
    public String bindLogin(String userId, String shopId, String tableId, String token) {
        if (!isEligible(userId, shopId, tableId) || !StringUtils.hasText(token)) {
            return null;
        }
        redisTemplate.opsForValue().set(key(userId, shopId, tableId), token, expireSeconds, TimeUnit.SECONDS);
        return token;
    }

    @Override
    public boolean validateLogin(String userId, String shopId, String tableId, String token) {
        if (!isEligible(userId, shopId, tableId) || !StringUtils.hasText(token)) {
            return false;
        }
        String stored = redisTemplate.opsForValue().get(key(userId, shopId, tableId));
        return token.equals(stored);
    }

    @Override
    public boolean isBound(String userId, String shopId, String tableId) {
        if (!isEligible(userId, shopId, tableId)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(userId, shopId, tableId)));
    }

    @Override
    public boolean isEligible(String userId, String shopId, String tableId) {
        return StringUtils.hasText(userId) && StringUtils.hasText(shopId) && StringUtils.hasText(tableId);
    }

    @Override
    public void removeLogin(String userId, String shopId, String tableId) {
        if (isEligible(userId, shopId, tableId)) {
            redisTemplate.delete(key(userId, shopId, tableId));
        }
    }

    @Override
    public long getExpireSeconds() {
        return expireSeconds;
    }

    private String key(String userId, String shopId, String tableId) {
        return keyPrefix + userId + ":" + shopId + ":" + tableId;
    }
}