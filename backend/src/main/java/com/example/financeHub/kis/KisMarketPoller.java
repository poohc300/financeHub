package com.example.financeHub.kis;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.financeHub.kis.model.KisMarketIndex;
import com.example.financeHub.websocket.StockPriceWebSocketHandler;

import jakarta.annotation.PostConstruct;

@Component
public class KisMarketPoller {

    private static final Logger log = LoggerFactory.getLogger(KisMarketPoller.class);

    // KIS 업종지수 코드: 0001=KOSPI 종합, 1001=KOSDAQ 종합
    private static final List<String[]> INDEX_CODES = List.of(
            new String[]{"0001", "코스피"},
            new String[]{"1001", "코스닥"}
    );

    @Value("${kis.base-url}")
    private String baseUrl;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    private final KisTokenManager tokenManager;
    private final StockPriceWebSocketHandler broadcastHandler;
    private final RestTemplate restTemplate;

    private volatile List<KisMarketIndex> marketCache = new ArrayList<>();

    public KisMarketPoller(KisTokenManager tokenManager,
                            StockPriceWebSocketHandler broadcastHandler,
                            RestTemplate restTemplate) {
        this.tokenManager = tokenManager;
        this.broadcastHandler = broadcastHandler;
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void init() {
        broadcastHandler.setMarketPoller(this);
        // 서버 시작 시 장 중이면 즉시 폴링
        ZoneId seoul = ZoneId.of("Asia/Seoul");
        boolean isWeekday = LocalDate.now(seoul).getDayOfWeek().getValue() <= 5;
        LocalTime now = LocalTime.now(seoul);
        if (isWeekday && now.isAfter(LocalTime.of(9, 0)) && now.isBefore(LocalTime.of(15, 30))) {
            pollMarket();
        }
    }

    public List<KisMarketIndex> getMarketCache() {
        return marketCache;
    }

    /** 평일 09:00~15:29 5분마다 KOSPI/KOSDAQ 현재지수 폴링 */
    @Scheduled(cron = "0 */5 9-15 * * MON-FRI", zone = "Asia/Seoul")
    public void scheduledPoll() {
        if (LocalTime.now(ZoneId.of("Asia/Seoul")).isAfter(LocalTime.of(15, 30))) return;
        pollMarket();
    }

    @SuppressWarnings("unchecked")
    public void pollMarket() {
        List<KisMarketIndex> result = new ArrayList<>();
        try {
            String token = tokenManager.getAccessToken();

            for (String[] entry : INDEX_CODES) {
                String iscd = entry[0];
                String name = entry[1];

                HttpHeaders headers = new HttpHeaders();
                headers.set("authorization", "Bearer " + token);
                headers.set("appkey", appKey);
                headers.set("appsecret", appSecret);
                headers.set("tr_id", "FHKUP03500100");
                headers.set("custtype", "P");

                String url = UriComponentsBuilder
                        .fromHttpUrl(baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-index-price")
                        .queryParam("fid_cond_mrkt_div_code", "U")
                        .queryParam("fid_input_iscd", iscd)
                        .build().toUriString();

                ResponseEntity<Map> response = restTemplate.exchange(
                        url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) continue;

                Map<String, Object> output = (Map<String, Object>) response.getBody().get("output");
                if (output == null) continue;

                result.add(KisMarketIndex.builder()
                        .idxNm(name)
                        .currentIdx(str(output, "bstp_nmix_prpr"))
                        .change(str(output, "bstp_nmix_prdy_vrss"))
                        .changeRate(str(output, "bstp_nmix_prdy_ctrt"))
                        .open(str(output, "bstp_nmix_oprc"))
                        .high(str(output, "bstp_nmix_hgpr"))
                        .low(str(output, "bstp_nmix_lwpr"))
                        .volume(str(output, "acml_vol"))
                        .build());
            }

            if (!result.isEmpty()) {
                marketCache = result;
                broadcastHandler.broadcastMarket(result);
                log.info("시장 지수 갱신: {}", result.stream().map(KisMarketIndex::getIdxNm).toList());
            }
        } catch (Exception e) {
            log.error("시장 지수 폴링 실패: {}", e.getMessage(), e);
        }
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }
}
