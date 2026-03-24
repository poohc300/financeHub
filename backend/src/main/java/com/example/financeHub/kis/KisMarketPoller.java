package com.example.financeHub.kis;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.financeHub.kis.model.KisMarketIndex;
import com.example.financeHub.websocket.StockPriceWebSocketHandler;

import jakarta.annotation.PostConstruct;

/**
 * KOSPI/KOSDAQ 업종지수 실시간 구독 관리.
 * KIS WebSocket H0UPCNT0 TR로 실시간 체결 데이터 수신 → broadcastMarket().
 * REST 폴링 방식은 종가(장 마감 후)만 제공하므로 WebSocket 방식 사용.
 */
@Component
public class KisMarketPoller {

    private static final Logger log = LoggerFactory.getLogger(KisMarketPoller.class);

    // 구독할 업종 코드: 0001=KOSPI, 1001=KOSDAQ
    private static final List<String> INDEX_CODES = List.of("0001", "1001");

    private final KisWebSocketClient kisWebSocketClient;
    private final StockPriceWebSocketHandler broadcastHandler;

    public KisMarketPoller(KisWebSocketClient kisWebSocketClient,
                            StockPriceWebSocketHandler broadcastHandler) {
        this.kisWebSocketClient = kisWebSocketClient;
        this.broadcastHandler = broadcastHandler;
    }

    @PostConstruct
    public void init() {
        broadcastHandler.setMarketPoller(this);
        // 서버 시작 시 장 중이면 즉시 구독
        ZoneId seoul = ZoneId.of("Asia/Seoul");
        boolean isWeekday = LocalDate.now(seoul).getDayOfWeek().getValue() <= 5;
        LocalTime now = LocalTime.now(seoul);
        if (isWeekday && now.isAfter(LocalTime.of(9, 0)) && now.isBefore(LocalTime.of(15, 30))) {
            subscribeAll();
        }
    }

    public List<KisMarketIndex> getMarketCache() {
        return new ArrayList<>(kisWebSocketClient.getIndexCache().values());
    }

    /** 평일 09:00~15:29 5분마다 구독 유지 확인 (재연결 대비) */
    @Scheduled(cron = "0 */5 9-15 * * MON-FRI", zone = "Asia/Seoul")
    public void scheduledSubscribe() {
        if (LocalTime.now(ZoneId.of("Asia/Seoul")).isAfter(LocalTime.of(15, 30))) return;
        subscribeAll();
    }

    private void subscribeAll() {
        for (String code : INDEX_CODES) {
            kisWebSocketClient.subscribeIndex(code);
        }
        log.info("업종지수 구독 확인: {}", INDEX_CODES);
    }
}
