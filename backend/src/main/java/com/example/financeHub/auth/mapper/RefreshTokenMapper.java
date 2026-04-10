package com.example.financeHub.auth.mapper;

import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.financeHub.auth.model.RefreshToken;

@Mapper
public interface RefreshTokenMapper {
    void insert(RefreshToken token);
    RefreshToken findByTokenHash(@Param("tokenHash") String tokenHash);
    /** 같은 family 전체 revoke (탈취 감지 시 호출) */
    void revokeByFamily(@Param("family") UUID family);
    /** 로그아웃 — 해당 토큰 단건 revoke */
    void revokeByTokenHash(@Param("tokenHash") String tokenHash);
    /** 만료된 토큰 정리 (스케줄러에서 주기적 호출) */
    void deleteExpired();
}
