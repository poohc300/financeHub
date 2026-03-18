package com.example.financeHub.kis;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.financeHub.kis.model.KisStockPrice;
import com.example.financeHub.websocket.StockPriceWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@Component
public class KisWebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(KisWebSocketClient.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Value("${kis.ws-url}")
    private String wsUrl;

    private final KisTokenManager tokenManager;
    private final StockPriceWebSocketHandler broadcastHandler;

    private WebSocket webSocket;
    private final Set<String> subscribedSymbols = ConcurrentHashMap.newKeySet();
    private volatile boolean connected = false;

    public KisWebSocketClient(KisTokenManager tokenManager, StockPriceWebSocketHandler broadcastHandler) {
        this.tokenManager = tokenManager;
        this.broadcastHandler = broadcastHandler;
    }

    @PostConstruct
    public void init() {
        // 순환 의존 해소: handler에 this 주입
        broadcastHandler.setKisWebSocketClient(this);
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
            log.info("KIS WebSocket 연결 완료: {}", wsUrl);
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
        log.info("KIS 종목 구독: {}", isuSrtCd);
    }

    public void unsubscribe(String isuSrtCd) {
        if (!subscribedSymbols.contains(isuSrtCd)) return;
        sendSubscribeMessage(isuSrtCd, "2");
        subscribedSymbols.remove(isuSrtCd);
        log.info("KIS 종목 구독 해제: {}", isuSrtCd);
    }

    private void sendSubscribeMessage(String isuSrtCd, String trType) {
        if (webSocket == null) return;
        try {
            String approvalKey = tokenManager.getApprovalKey();
            String msg = String.format(
                    "{\"header\":{\"approval_key\":\"%s\",\"custtype\":\"P\",\"tr_type\":\"%s\",\"content-type\":\"utf-8\"}," +
                    "\"body\":{\"input\":{\"tr_id\":\"H0STCNT0\",\"tr_key\":\"%s\"}}}",
                    approvalKey, trType, isuSrtCd);
            webSocket.sendText(msg, true);
        } catch (Exception e) {
            log.error("KIS 구독 메시지 전송 실패: {}", e.getMessage());
        }
    }

    private void handleMessage(String message) {
        try {
            // PINGPONG 처리
            if (message.contains("PINGPONG")) {
                webSocket.sendText(message, true);
                return;
            }

            // 데이터 형식: 0|H0STCNT0|001|데이터
            String[] parts = message.split("\\|");
            if (parts.length < 4) return;
            if (!"H0STCNT0".equals(parts[1])) return;

            String[] fields = parts[3].split("\\^");
            if (fields.length < 9) return;

            KisStockPrice price = KisStockPrice.builder()
                    .isuSrtCd(fields[0])
                    .time(fields[1])
                    .currentPrice(fields[2])
                    .change(fields[3])
                    .changeRate(fields[4])
                    .open(fields[5])
                    .high(fields[6])
                    .low(fields[7])
                    .volume(fields[8])
                    .build();

            broadcastHandler.broadcastPrice(price);

        } catch (Exception e) {
            log.error("KIS 메시지 파싱 오류: {}", e.getMessage());
        }
    }

    private class KisWebSocketListener implements WebSocket.Listener {

        private final String approvalKey;
        private final StringBuilder buffer = new StringBuilder();

        KisWebSocketListener(String approvalKey) {
            this.approvalKey = approvalKey;
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
        public void onOpen(WebSocket ws) {
            log.info("KIS WS onOpen");
            ws.request(1);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            log.warn("KIS WS 연결 종료: {} {}", statusCode, reason);
            connected = false;
            // 재연결 시 기존 구독 심볼 다시 구독
            if (!subscribedSymbols.isEmpty()) {
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        tokenManager.resetApprovalKey();
                        ensureConnected();
                        for (String symbol : subscribedSymbols) {
                            sendSubscribeMessage(symbol, "1");
                        }
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
