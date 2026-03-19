package com.example.financeHub.kis;

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

import com.example.financeHub.kis.model.KisRankingItem;
import com.example.financeHub.websocket.StockPriceWebSocketHandler;

import jakarta.annotation.PostConstruct;

@Component
public class KisRankingPoller {

    private static final Logger log = LoggerFactory.getLogger(KisRankingPoller.class);
    private static final int TOP_N = 5;

    @Value("${kis.base-url}")
    private String baseUrl;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    private final KisTokenManager tokenManager;
    private final KisWebSocketClient kisWebSocketClient;
    private final StockPriceWebSocketHandler broadcastHandler;
    private final RestTemplate restTemplate;

    // 현재 랭킹 메모리 캐시 (DB 미사용)
    private volatile List<KisRankingItem> rankingCache = new ArrayList<>();

    public KisRankingPoller(KisTokenManager tokenManager,
                             KisWebSocketClient kisWebSocketClient,
                             StockPriceWebSocketHandler broadcastHandler,
                             RestTemplate restTemplate) {
        this.tokenManager = tokenManager;
        this.kisWebSocketClient = kisWebSocketClient;
        this.broadcastHandler = broadcastHandler;
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void init() {
        broadcastHandler.setRankingPoller(this);
        // 서버 시작 시 장 중이면 즉시 폴링
        java.time.LocalTime now = java.time.LocalTime.now(java.time.ZoneId.of("Asia/Seoul"));
        boolean isWeekday = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul"))
                .getDayOfWeek().getValue() <= 5;
        if (isWeekday && now.isAfter(java.time.LocalTime.of(9, 0))
                && now.isBefore(java.time.LocalTime.of(15, 30))) {
            pollRanking();
        }
    }

    public List<KisRankingItem> getRankingCache() {
        return rankingCache;
    }

    /** 평일 09:00~15:29 사이 5분마다 순위 폴링 */
    @Scheduled(cron = "0 */5 9-15 * * MON-FRI", zone = "Asia/Seoul")
    public void scheduledPoll() {
        java.time.LocalTime now = java.time.LocalTime.now(java.time.ZoneId.of("Asia/Seoul"));
        if (now.isAfter(java.time.LocalTime.of(15, 30))) return;
        pollRanking();
    }

    @SuppressWarnings("unchecked")
    public void pollRanking() {
        try {
            String token = tokenManager.getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", "Bearer " + token);
            headers.set("appkey", appKey);
            headers.set("appsecret", appSecret);
            headers.set("tr_id", "FHPST01700000");
            headers.set("custtype", "P");

            String url = UriComponentsBuilder
                    .fromHttpUrl(baseUrl + "/uapi/domestic-stock/v1/ranking/fluctuation")
                    .queryParam("fid_cond_mrkt_div_code", "J")
                    .queryParam("fid_cond_scr_div_code", "20170")
                    .queryParam("fid_input_iscd", "0000")
                    .queryParam("fid_rank_sort_cls_code", "0")
                    .queryParam("fid_input_cnt_1", "0")
                    .queryParam("fid_prc_cls_code", "1")
                    .queryParam("fid_input_price_1", "")
                    .queryParam("fid_input_price_2", "")
                    .queryParam("fid_vol_cnt", "")
                    .queryParam("fid_trgt_cls_code", "0")
                    .queryParam("fid_trgt_exls_cls_code", "0")
                    .queryParam("fid_div_cls_code", "0")
                    .queryParam("fid_rsfl_rate2", "")
                    .queryParam("fid_rsfl_rate1", "")
                    .build().toUriString();

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) return;

            List<Map<String, Object>> output = (List<Map<String, Object>>) response.getBody().get("output");
            if (output == null || output.isEmpty()) return;

            List<KisRankingItem> newRanking = new ArrayList<>();
            for (Map<String, Object> item : output.subList(0, Math.min(TOP_N, output.size()))) {
                newRanking.add(KisRankingItem.builder()
                        .isuSrtCd(str(item, "stck_shrn_iscd"))
                        .isuNm(str(item, "hts_kor_isnm"))
                        .currentPrice(str(item, "stck_prpr"))
                        .change(str(item, "prdy_vrss"))
                        .changeRate(str(item, "prdy_ctrt"))
                        .volume(str(item, "acml_vol"))
                        .build());
            }

            // 구독 종목 동적 교체
            List<String> newSymbols = newRanking.stream().map(KisRankingItem::getIsuSrtCd).toList();
            kisWebSocketClient.updateSubscriptions(newSymbols);

            rankingCache = newRanking;

            // 프론트에 랭킹 브로드캐스트
            broadcastHandler.broadcastRanking(newRanking);
            log.info("TOP {} 랭킹 갱신: {}", TOP_N, newSymbols);

        } catch (Exception e) {
            log.error("랭킹 폴링 실패: {}", e.getMessage(), e);
        }
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }
}
