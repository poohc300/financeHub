package com.example.financeHub.overseas.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * USD/KRW 환율 서비스 — frankfurter.app (ECB 기준) 1시간 캐시
 */
@Service
public class ExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);
    private static final String API_URL = "https://api.frankfurter.app/latest?from=USD&to=KRW";

    private final RestTemplate restTemplate;

    private BigDecimal usdKrw = BigDecimal.ZERO;
    private LocalDateTime lastUpdated;

    public ExchangeRateService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public BigDecimal getUsdKrw() {
        if (usdKrw.compareTo(BigDecimal.ZERO) == 0 || lastUpdated == null) {
            refresh();
        }
        return usdKrw;
    }

    /** 매 1시간마다 자동 갱신 */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void refresh() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(API_URL, Map.class);
            if (response == null) return;

            @SuppressWarnings("unchecked")
            Map<String, Object> rates = (Map<String, Object>) response.get("rates");
            if (rates == null || !rates.containsKey("KRW")) return;

            usdKrw = new BigDecimal(rates.get("KRW").toString());
            lastUpdated = LocalDateTime.now();
            log.info("USD/KRW 환율 갱신: {} ({})", usdKrw, lastUpdated);
        } catch (Exception e) {
            log.warn("환율 조회 실패: {}", e.getMessage());
        }
    }
}
