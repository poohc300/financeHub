package com.example.financeHub.kis;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.financeHub.kis.model.KisFuturesPrice;
import com.example.financeHub.websocket.StockPriceWebSocketHandler;

import jakarta.annotation.PostConstruct;

/**
 * KOSPI200 선물 근월물 실시간 구독 관리.
 * KIS WebSocket H0IFCNT0 TR로 선물 체결 데이터 수신.
 * 선물 거래 시간: 09:00 ~ 15:45 (주식보다 15분 더 연장)
 */
@Component
public class KisFuturesPoller {

    private static final Logger log = LoggerFactory.getLogger(KisFuturesPoller.class);

    private final KisWebSocketClient kisWebSocketClient;
    private final StockPriceWebSocketHandler broadcastHandler;

    public KisFuturesPoller(KisWebSocketClient kisWebSocketClient,
                             StockPriceWebSocketHandler broadcastHandler) {
        this.kisWebSocketClient = kisWebSocketClient;
        this.broadcastHandler = broadcastHandler;
    }

    @PostConstruct
    public void init() {
        broadcastHandler.setFuturesPoller(this);
        ZoneId seoul = ZoneId.of("Asia/Seoul");
        boolean isWeekday = LocalDate.now(seoul).getDayOfWeek().getValue() <= 5;
        LocalTime now = LocalTime.now(seoul);
        if (isWeekday && now.isAfter(LocalTime.of(9, 0)) && now.isBefore(LocalTime.of(15, 46))) {
            subscribeNearMonth();
        }
    }

    public List<KisFuturesPrice> getFuturesCache() {
        return new ArrayList<>(kisWebSocketClient.getFuturesCache().values());
    }

    /** 평일 09:00~15:45 5분마다 구독 유지 확인 (재연결 대비) */
    @Scheduled(cron = "0 */5 9-15 * * MON-FRI", zone = "Asia/Seoul")
    public void scheduledSubscribe() {
        if (LocalTime.now(ZoneId.of("Asia/Seoul")).isAfter(LocalTime.of(15, 46))) return;
        subscribeNearMonth();
    }

    public void subscribeNearMonth() {
        String code = getNearMonthCode();
        kisWebSocketClient.subscribeFutures(code);
        log.info("KOSPI200 선물 근월물 구독: {}", code);
    }

    /**
     * KOSPI200 선물 근월물 종목코드 계산.
     * 만기: 매 3, 6, 9, 12월 두 번째 목요일.
     * KIS TR 키 형식: 101W + YY(2자리) + MM(2자리), 예: 101W2606
     */
    public static String getNearMonthCode() {
        ZoneId seoul = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(seoul);
        int[] expiryMonths = {3, 6, 9, 12};

        for (int m : expiryMonths) {
            if (m < today.getMonthValue()) continue;
            LocalDate expiry = getSecondThursday(today.getYear(), m);
            if (!today.isAfter(expiry)) {
                int yy = today.getYear() % 100;
                return String.format("101W%02d%02d", yy, m);
            }
        }
        // 12월 만기도 지났으면 다음 해 3월
        int yy = (today.getYear() + 1) % 100;
        return String.format("101W%02d03", yy);
    }

    private static LocalDate getSecondThursday(int year, int month) {
        LocalDate firstOfMonth = LocalDate.of(year, month, 1);
        LocalDate firstThursday = firstOfMonth.with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY));
        return firstThursday.plusWeeks(1);
    }
}
