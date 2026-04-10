package com.example.financeHub.auth.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

/**
 * JWT Access Token 생성 / 검증 / Claims 파싱.
 * Refresh Token은 opaque UUID로 처리 (AuthService 담당).
 */
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretString;

    @Value("${jwt.access_token_expiration}")
    private long accessTokenExpiration;   // 밀리초 (기본 900000 = 15분)

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
    }

    /** Access Token 생성 */
    public String generateAccessToken(Long userId, String username,
                                      List<String> roles, List<String> permissions) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(username)
                .claim("userId",      userId)
                .claim("roles",       roles)
                .claim("permissions", permissions)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /** 토큰에서 username 추출 */
    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /** 토큰 유효성 검사 (서명 + 만료) */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
