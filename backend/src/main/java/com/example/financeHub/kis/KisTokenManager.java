package com.example.financeHub.kis;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class KisTokenManager {

    private static final Logger log = LoggerFactory.getLogger(KisTokenManager.class);

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    @Value("${kis.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    private String accessToken;
    private LocalDateTime tokenExpiry;
    private String approvalKey;

    public KisTokenManager(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /** 평일 08:50 — 장 시작 10분 전 토큰 강제 갱신 */
    @Scheduled(cron = "0 50 8 * * MON-FRI", zone = "Asia/Seoul")
    public void refreshTokenBeforeMarketOpen() {
        log.info("장 시작 전 KIS 토큰 강제 갱신");
        issueAccessToken();
    }

    public synchronized String getAccessToken() {
        if (accessToken == null || LocalDateTime.now().isAfter(tokenExpiry)) {
            issueAccessToken();
        }
        if (accessToken == null) {
            throw new RuntimeException("KIS access_token 없음 — 발급 실패");
        }
        return accessToken;
    }

    public synchronized String getApprovalKey() {
        if (approvalKey == null) {
            issueApprovalKey();
        }
        return approvalKey;
    }

    public synchronized void resetApprovalKey() {
        approvalKey = null;
    }

    @SuppressWarnings("unchecked")
    private void issueAccessToken() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("grant_type", "client_credentials");
            body.put("appkey", appKey);
            body.put("appsecret", appSecret);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
            Map<String, Object> response = restTemplate.postForObject(
                    baseUrl + "/oauth2/tokenP", entity, Map.class);

            if (response == null) throw new RuntimeException("KIS 토큰 응답이 null");

            this.accessToken = (String) response.get("access_token");
            int expiresIn = Integer.parseInt(response.get("expires_in").toString());
            this.tokenExpiry = LocalDateTime.now().plusSeconds(expiresIn - 300);
            log.info("KIS access_token 발급 완료, 만료: {}", tokenExpiry);
        } catch (Exception e) {
            log.error("KIS access_token 발급 실패: {}", e.getMessage(), e);
            throw new RuntimeException("KIS 토큰 발급 실패", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void issueApprovalKey() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("grant_type", "client_credentials");
            body.put("appkey", appKey);
            body.put("secretkey", appSecret);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
            Map<String, Object> response = restTemplate.postForObject(
                    baseUrl + "/oauth2/Approval", entity, Map.class);

            if (response == null) throw new RuntimeException("KIS approval_key 응답이 null");

            this.approvalKey = (String) response.get("approval_key");
            log.info("KIS approval_key 발급 완료");
        } catch (Exception e) {
            log.error("KIS approval_key 발급 실패: {}", e.getMessage(), e);
            throw new RuntimeException("KIS approval_key 발급 실패", e);
        }
    }
}
