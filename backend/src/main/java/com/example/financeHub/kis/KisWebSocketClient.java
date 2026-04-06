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

import com.example.financeHub.kis.model.KisMarketIndex;
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
    // 업종지수 실시간 캐시 (H0UPCNT0)
    private final Map<String, KisMarketIndex> indexCache = new ConcurrentHashMap<>();

    private WebSocket webSocket;
    private final Set<String> subscribedSymbols = ConcurrentHashMap.newKeySet();
    private final Set<String> subscribedIndexCodes = ConcurrentHashMap.newKeySet();
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

    public Map<String, KisMarketIndex> getIndexCache() {
        return indexCache;
    }

    /** 업종지수 실시간 구독 (H0UPCNT0) — 0001=KOSPI, 1001=KOSDAQ */
    public void subscribeIndex(String indexCode) {
        ensureConnected();
        if (subscribedIndexCodes.contains(indexCode)) return;
        sendMessageWithTrId("H0UPCNT0", indexCode, "1");
        subscribedIndexCodes.add(indexCode);
        log.info("업종지수 구독: {}", indexCode);
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
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();
            webSocket = client.newWebSocketBuilder()
                    .buildAsync(URI.create(wsUrl), new KisWebSocketListener(approvalKey))
                    .orTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .join();
            connected = true;
            log.info("KIS WebSocket 연결: {}", wsUrl);
        } catch (Exception e) {
            connected = false;
            log.error("KIS WebSocket 연결 실패 (15초 타임아웃): {}", e.getMessage());
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
        sendMessageWithTrId("H0STCNT0", isuSrtCd, trType);
    }

    private void sendMessageWithTrId(String trId, String trKey, String trType) {
        if (webSocket == null) return;
        try {
            String approvalKey = tokenManager.getApprovalKey();
            String msg = String.format(
                    "{\"header\":{\"approval_key\":\"%s\",\"custtype\":\"P\",\"tr_type\":\"%s\",\"content-type\":\"utf-8\"},"
                    + "\"body\":{\"input\":{\"tr_id\":\"%s\",\"tr_key\":\"%s\"}}}",
                    approvalKey, trType, trId, trKey);
            webSocket.sendText(msg, true);
        } catch (Exception e) {
            log.error("메시지 전송 실패 trId={}: {}", trId, e.getMessage());
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
            if (parts.length < 4) return;

            // 업종지수 실시간 (H0UPCNT0)
            if ("H0UPCNT0".equals(parts[1])) {
                String[] f = parts[3].split("\\^");
                if (f.length < 5) return;
                // f[0]=업종코드, f[1]=시간, f[2]=현재지수, f[3]=전일대비부호(1상승/4하한/5하락),
                // f[4]=전일대비(포인트), f[5]= KIS 포맷 불확실(거래량일 수 있음) → 직접 계산
                log.debug("H0UPCNT0 raw fields: {}", String.join("|", f));
                String code = f[0];
                String idxNm = "0001".equals(code) ? "코스피" : "1001".equals(code) ? "코스닥" : code;
                String sign = f[3];
                boolean negative = "4".equals(sign) || "5".equals(sign);
                String change = negative && !f[4].startsWith("-") ? "-" + f[4] : f[4];
                // f[5]가 등락률인지 거래량인지 KIS 포맷 불확실 → 현재지수와 전일대비로 직접 계산
                String changeRate;
                try {
                    double currentVal = Double.parseDouble(f[2]);
                    double changeVal = Double.parseDouble(change);
                    double prevVal = currentVal - changeVal;
                    changeRate = prevVal != 0 ? String.format("%.2f", (changeVal / prevVal) * 100) : "0.00";
                } catch (NumberFormatException e) {
                    changeRate = "0.00";
                }

                KisMarketIndex idx = KisMarketIndex.builder()
                        .idxNm(idxNm)
                        .currentIdx(f[2])
                        .change(change)
                        .changeRate(changeRate)
                        .open(f.length > 6 ? f[6] : "")
                        .high(f.length > 7 ? f[7] : "")
                        .low(f.length > 8 ? f[8] : "")
                        .volume(f.length > 9 ? f[9] : "")
                        .build();

                indexCache.put(code, idx);
                broadcastHandler.broadcastMarket(new java.util.ArrayList<>(indexCache.values()));
                log.debug("업종지수 갱신: {} {}", idxNm, f[2]);
                return;
            }

            if (!"H0STCNT0".equals(parts[1])) return;

            String[] f = parts[3].split("\\^");
            if (f.length < 10) return;
            // KIS H0STCNT0 실제 포맷:
            // f[0]=종목코드, f[1]=체결시간, f[2]=현재가, f[3]=전일대비부호(1상한/2상승/3보합/4하한/5하락),
            // f[4]=전일대비(절대), f[5]=등락률, f[6]=가중평균가, f[7]=시가, f[8]=고가, f[9]=저가,
            // f[13]=누적거래량
            log.debug("H0STCNT0 raw: {}", String.join("|", java.util.Arrays.copyOf(f, Math.min(f.length, 15))));
            String stockSign = f[3];
            boolean stockNeg = "4".equals(stockSign) || "5".equals(stockSign);
            String stockChange = stockNeg && !f[4].startsWith("-") ? "-" + f[4] : f[4];
            String stockChangeRate = stockNeg && !f[5].startsWith("-") ? "-" + f[5] : f[5];

            KisStockPrice price = KisStockPrice.builder()
                    .isuSrtCd(f[0])
                    .time(f[1])
                    .currentPrice(f[2])
                    .change(stockChange)
                    .changeRate(stockChangeRate)
                    .open(f[7])
                    .high(f[8])
                    .low(f[9])
                    .volume(f.length > 13 ? f[13] : "")
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
                        for (String idxCode : subscribedIndexCodes) {
                            sendMessageWithTrId("H0UPCNT0", idxCode, "1");
                        }
                        log.info("KIS WS 재연결 후 {}종목 + {}지수 재구독",
                                subscribedSymbols.size(), subscribedIndexCodes.size());
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
