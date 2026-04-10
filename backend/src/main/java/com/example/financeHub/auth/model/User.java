package com.example.financeHub.auth.model;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private Long id;
    private String username;
    private String password;       // BCrypt hash (LOCAL 전용, OAuth2는 null)
    private String email;
    private String loginType;      // LOCAL | GOOGLE | KAKAO
    private boolean enabled;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // 조회 시 함께 로드 (MyBatis resultMap)
    private List<Role> roles;
    private List<String> permissions;
}
