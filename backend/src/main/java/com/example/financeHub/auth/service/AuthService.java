package com.example.financeHub.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.example.financeHub.auth.dto.LoginRequest;
import com.example.financeHub.auth.dto.LoginResponse;
import com.example.financeHub.auth.dto.UserInfoResponse;
import com.example.financeHub.auth.mapper.RefreshTokenMapper;
import com.example.financeHub.auth.mapper.UserMapper;
import com.example.financeHub.auth.model.RefreshToken;
import com.example.financeHub.auth.model.User;
import com.example.financeHub.auth.security.CustomUserDetails;
import com.example.financeHub.auth.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;

    @Value("${jwt.refresh_token_expiration}")
    private long refreshTokenExpiration;  // 밀리초

    // ───────────────────────────────────────────────────────────────
    // 로그인
    // ───────────────────────────────────────────────────────────────
    public LoginResponse login(LoginRequest request) {
        // Spring Security 인증 (BCrypt 검증 포함)
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

        String accessToken = jwtTokenProvider.generateAccessToken(
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getRoleNames(),
                userDetails.getPermissionNames());

        log.info("로그인 성공: {}", userDetails.getUsername());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration() / 1000)
                .username(userDetails.getUsername())
                .roles(userDetails.getRoleNames())
                .permissions(userDetails.getPermissionNames())
                .build();
    }

    // ───────────────────────────────────────────────────────────────
    // Refresh Token 발급 (로그인 성공 후 별도 호출)
    // ───────────────────────────────────────────────────────────────
    public String issueRefreshToken(String username) {
        User user = userMapper.findByUsername(username);
        String rawToken   = UUID.randomUUID().toString();
        String tokenHash  = sha256(rawToken);
        UUID   family     = UUID.randomUUID();
        OffsetDateTime expiresAt = OffsetDateTime.now()
                .plusSeconds(refreshTokenExpiration / 1000);

        refreshTokenMapper.insert(RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .family(family)
                .expiresAt(expiresAt)
                .build());

        return rawToken;
    }

    // ───────────────────────────────────────────────────────────────
    // Access Token 갱신 (Refresh Token Rotation)
    // ───────────────────────────────────────────────────────────────
    public LoginResponse refresh(String rawRefreshToken) {
        String tokenHash = sha256(rawRefreshToken);
        RefreshToken stored = refreshTokenMapper.findByTokenHash(tokenHash);

        if (stored == null) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        // 이미 revoke된 토큰 → 탈취 가능성 → family 전체 무효화
        if (stored.isRevoked()) {
            log.warn("Revoked Refresh Token 재사용 감지! family={} 전체 무효화", stored.getFamily());
            refreshTokenMapper.revokeByFamily(stored.getFamily());
            throw new SecurityException("보안 위협 감지: 이미 사용된 토큰입니다. 다시 로그인하세요.");
        }

        // 만료 체크
        if (stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Refresh Token이 만료되었습니다. 다시 로그인하세요.");
        }

        // 기존 토큰 revoke (Rotation)
        refreshTokenMapper.revokeByTokenHash(tokenHash);

        // 사용자 정보 로드
        User user = userMapper.findById(stored.getUserId());

        // 새 Access Token 생성
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getUsername(),
                user.getRoles().stream().map(r -> r.getName()).toList(),
                user.getPermissions());

        // 새 Refresh Token 발급 (같은 family 유지)
        String newRawToken = UUID.randomUUID().toString();
        OffsetDateTime expiresAt = OffsetDateTime.now()
                .plusSeconds(refreshTokenExpiration / 1000);

        refreshTokenMapper.insert(RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(sha256(newRawToken))
                .family(stored.getFamily())   // 같은 family
                .expiresAt(expiresAt)
                .build());

        log.info("토큰 갱신: userId={}", user.getId());

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration() / 1000)
                .username(user.getUsername())
                .roles(user.getRoles().stream().map(r -> r.getName()).toList())
                .permissions(user.getPermissions())
                .newRefreshToken(newRawToken)   // 컨트롤러에서 쿠키 세팅에 사용
                .build();
    }

    // ───────────────────────────────────────────────────────────────
    // 로그아웃
    // ───────────────────────────────────────────────────────────────
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;
        String tokenHash = sha256(rawRefreshToken);
        refreshTokenMapper.revokeByTokenHash(tokenHash);
        log.info("로그아웃: 토큰 revoke 완료");
    }

    // ───────────────────────────────────────────────────────────────
    // 현재 사용자 정보
    // ───────────────────────────────────────────────────────────────
    public UserInfoResponse getMe(String username) {
        User user = userMapper.findByUsername(username);
        return UserInfoResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .loginType(user.getLoginType())
                .roles(user.getRoles().stream().map(r -> r.getName()).toList())
                .permissions(user.getPermissions())
                .build();
    }

    // ───────────────────────────────────────────────────────────────
    // 만료 토큰 정리 (스케줄러)
    // ───────────────────────────────────────────────────────────────
    public void cleanupExpiredTokens() {
        refreshTokenMapper.deleteExpired();
    }

    // ───────────────────────────────────────────────────────────────
    // SHA-256 해시 (Refresh Token 원본을 DB에 저장하지 않기 위함)
    // ───────────────────────────────────────────────────────────────
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 해시 실패", e);
        }
    }
}
