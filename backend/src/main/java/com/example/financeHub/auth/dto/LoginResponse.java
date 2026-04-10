package com.example.financeHub.auth.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String tokenType;       // "Bearer"
    private long expiresIn;         // 초 단위 (900)
    private String username;
    private List<String> roles;
    private List<String> permissions;

    @JsonIgnore                     // 클라이언트 응답 JSON에는 포함하지 않음
    private String newRefreshToken; // refresh 흐름에서 컨트롤러가 쿠키 세팅에 사용
}
