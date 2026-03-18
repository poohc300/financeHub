package com.example.financeHub.kis;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.financeHub.kis.model.KisStockPrice;
import com.example.financeHub.websocket.StockPriceWebSocketHandler;

import jakarta.annotation.PostConstruct;

@Component
public class KisWebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(KisWebSocketClient.class);

    @Value("${kis.ws-url}")
    private String wsUrl;

    private final KisTokenManager tokenManager;
    private final StockPriceWebSocketHandler broadcastHandler;

    // 실시간 가격 메모리 캐시 (DB 미사용)
    private final Map<String, KisStockPrice> priceCache = new ConcurrentHashMap<>();

    private WebSocket webSocket;
    private final Set<String> subscribedSymbols = ConcurrentHashMap.newKeySet();
    private volatile boolean connected = false;

    public KisWebSocketClient(KisTokenManager tokenManager,
                               StockPriceWebSocketHandler broadcastHandler) {
        this.tokenManager = tokenManager;
        this.broadcastHandler = broadcastHandler;
    }

    @PostConstruct
    public void init() {
        broadcastHandler.setKisWebSocketClient(this);
    }

    /** 매 영업일 15:30 — 장 마감, 전체 구독 해제 */
    @Scheduled(cron = "0 30 15 * * MON-FRI", zone = "Asia/Seoul")
    public void unsubscribeAll() {
        for (String symbol : List.copyOf(subscribedSymbols)) {
            unsubscribe(symbol);
        }
        log.info("장 마감 — 전체 KIS 구독 해제");
    }

    public Map<String, KisStockPrice> getPriceCache() {
        return priceCache;
    }

    /**
     * 구독 종목 목록을 새 목록으로 교체.
     * 추가된 종목은 구독, 제거된 종목은 해제.
     * DB IO 없음 — 메모리만 사용.
     */
    public void updateSubscriptions(List<String> newSymbols) {
        ensureConnected();
        // 해제: 기존에 있지만 새 목록에 없는 종목
        for (String symbol : List.copyOf(subscribedSymbols)) {
            if (!newSymbols.contains(symbol)) {
                unsubscribe(symbol);
            }
        }
        // 구독: 새 목록에 있지만 기존에 없는 종목
        for (String symbol : newSymbols) {
            if (!subscribedSymbols.contains(symbol)) {
                subscribe(symbol);
            }
        }
    }

    private synchronized void ensureConnected() {
        if (connected && webSocket != null) return;
        try {
            String approvalKey = tokenManager.getApprovalKey();
            HttpClient client = HttpClient.newHttpClient();
            webSocket = client.newWebSocketBuilder()
                    .buildAsync(URI.create(wsUrl), new KisWebSocketListener(approvalKey))
                    .join();
            connected = true;
            log.info("KIS WebSocket 연결: {}", wsUrl);
        } catch (Exception e) {
            connected = false;
            log.error("KIS WebSocket 연결 실패: {}", e.getMessage(), e);
        }
    }

    public void subscribe(String isuSrtCd) {
        ensureConnected();
        if (subscribedSymbols.contains(isuSrtCd)) return;
        sendSubscribeMessage(isuSrtCd, "1");
        subscribedSymbols.add(isuSrtCd);
        log.info("구독: {}", isuSrtCd);
    }

    public void unsubscribe(String isuSrtCd) {
        if (!subscribedSymbols.contains(isuSrtCd)) return;
        sendSubscribeMessage(isuSrtCd, "2");
        subscribedSymbols.remove(isuSrtCd);
        priceCache.remove(isuSrtCd);
        log.info("구독 해제: {}", isuSrtCd);
    }

    private void sendSubscribeMessage(String isuSrtCd, String trType) {
        if (webSocket == null) return;
        try {
            String approvalKey = tokenManager.getApprovalKey();
            String msg = String.format(
                    "{\"header\":{\"approval_key\":\"%s\",\"custtype\":\"P\",\"tr_type\":\"%s\",\"content-type\":\"utf-8\"},"
                    + "\"body\":{\"input\":{\"tr_id\":\"H0STCNT0\",\"tr_key\":\"%s\"}}}",
                    approvalKey, trType, isuSrtCd);
            webSocket.sendText(msg, true);
        } catch (Exception e) {
            log.error("구독 메시지 전송 실패: {}", e.getMessage());
        }
    }

    private void handleMessage(String message) {
        try {
            if (message.contains("PINGPONG")) {
                webSocket.sendText(message, true);
                return;
            }
            // 형식: 0|H0STCNT0|001|종목코드^체결시간^현재가^전일대비^등락률^시가^고가^저가^누적거래량^...
            String[] parts = message.split("\\|");
            if (parts.length < 4 || !"H0STCNT0".equals(parts[1])) return;

            String[] f = parts[3].split("\\^");
            if (f.length < 9) return;

            KisStockPrice price = KisStockPrice.builder()
                    .isuSrtCd(f[0])
                    .time(f[1])
                    .currentPrice(f[2])
                    .change(f[3])
                    .changeRate(f[4])
                    .open(f[5])
                    .high(f[6])
                    .low(f[7])
                    .volume(f[8])
                    .build();

            // DB 쓰기 없이 메모리에만 저장
            priceCache.put(price.getIsuSrtCd(), price);
            broadcastHandler.broadcastPrice(price);

        } catch (Exception e) {
            log.error("메시지 파싱 오류: {}", e.getMessage());
        }
    }

    private class KisWebSocketListener implements WebSocket.Listener {

        private final String approvalKey;
        private final StringBuilder buffer = new StringBuilder();

        KisWebSocketListener(String approvalKey) {
            this.approvalKey = approvalKey;
        }

        @Override
        public void onOpen(WebSocket ws) {
            log.info("KIS WS onOpen");
            ws.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String message = buffer.toString();
                buffer.setLength(0);
                handleMessage(message);
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            log.warn("KIS WS 종료: {} {}", statusCode, reason);
            connected = false;
            if (!subscribedSymbols.isEmpty()) {
                new Thread(() -> {
                    try {
                        Thread.sleep(5000);
                        tokenManager.resetApprovalKey();
                        ensureConnected();
                        for (String symbol : subscribedSymbols) {
                            sendSubscribeMessage(symbol, "1");
                        }
                        log.info("KIS WS 재연결 후 {}종목 재구독", subscribedSymbols.size());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            log.error("KIS WS 오류: {}", error.getMessage());
            connected = false;
        }
    }
}
