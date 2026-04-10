package com.example.financeHub.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.financeHub.auth.dto.LoginRequest;
import com.example.financeHub.auth.dto.LoginResponse;
import com.example.financeHub.auth.dto.UserInfoResponse;
import com.example.financeHub.auth.security.CustomUserDetails;
import com.example.financeHub.auth.service.AuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE = "refresh_token";

    private final AuthService authService;

    /**
     * POST /api/auth/login
     * Body: { username, password }
     * Response: LoginResponse (Access Token) + Set-Cookie: refresh_token (httpOnly)
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);

        // Refresh Token 발급 후 httpOnly Cookie 세팅
        String rawRefreshToken = authService.issueRefreshToken(request.getUsername());
        ResponseCookie cookie  = buildRefreshCookie(rawRefreshToken, 7 * 24 * 60 * 60L);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    /**
     * POST /api/auth/refresh
     * Cookie: refresh_token
     * Response: 새 LoginResponse (새 Access Token) + 새 Set-Cookie (Refresh Token Rotation)
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String rawRefreshToken) {

        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return ResponseEntity.status(401).build();
        }

        // refresh() 내부에서 Rotation 처리 — newRefreshToken 포함 반환
        LoginResponse response = authService.refresh(rawRefreshToken);
        ResponseCookie cookie  = buildRefreshCookie(response.getNewRefreshToken(), 7 * 24 * 60 * 60L);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    /**
     * POST /api/auth/logout
     * Cookie: refresh_token → revoke 후 쿠키 삭제
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String rawRefreshToken) {

        authService.logout(rawRefreshToken);
        ResponseCookie deleteCookie = buildRefreshCookie("", 0L);  // maxAge=0 → 삭제

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .build();
    }

    /**
     * GET /api/auth/me
     * Authorization: Bearer {accessToken}
     * Response: 현재 사용자 정보
     */
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> me(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserInfoResponse info = authService.getMe(userDetails.getUsername());
        return ResponseEntity.ok(info);
    }

    // ─── httpOnly Refresh Token 쿠키 빌더 ─────────────────────────
    private ResponseCookie buildRefreshCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(true)          // HTTPS 전용
                .path("/api/auth")     // /api/auth 경로에서만 전송
                .maxAge(maxAgeSeconds)
                .sameSite("Strict")
                .build();
    }
}
