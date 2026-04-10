package com.example.financeHub.auth.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefreshToken {
    private Long id;
    private Long userId;
    private String tokenHash;       // SHA-256 해시 (원본 저장 X)
    private UUID family;            // Rotation Family 추적
    private OffsetDateTime expiresAt;
    private boolean revoked;
    private OffsetDateTime createdAt;
}
