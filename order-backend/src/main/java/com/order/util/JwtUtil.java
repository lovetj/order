package com.order.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 生成 token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param role     角色 customer / merchant
     */
    public String generateToken(String userId, String username, String role) {
        return generateToken(userId, username, role, null);
    }

    /**
     * 生成 token（携带所属店铺，用于店家端多店铺数据隔离）
     *
     * @param shopId 店家所属店铺ID（顾客端可为空）
     */
    public String generateToken(String userId, String username, String role, String shopId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        if (shopId != null && !shopId.trim().isEmpty()) {
            claims.put("shopId", shopId.trim());
        }
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return false;
            }
            Claims claims = parseToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public String getUserId(String token) {
        Claims claims = parseToken(token);
        Object userIdObj = claims.get("userId");
        if (userIdObj != null) {
            return String.valueOf(userIdObj);
        }
        return claims.get("userId", String.class);
    }

    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    public String getRole(String token) {
        Claims claims = parseToken(token);
        Object role = claims.get("role");
        return role == null ? null : String.valueOf(role);
    }

    /** 解析 token 中携带的店铺ID（店家端多店铺隔离用），无则返回 null */
    public String getShopId(String token) {
        if (!validateToken(token)) {
            return null;
        }
        Claims claims = parseToken(token);
        Object shopId = claims.get("shopId");
        return shopId == null ? null : String.valueOf(shopId);
    }
}
