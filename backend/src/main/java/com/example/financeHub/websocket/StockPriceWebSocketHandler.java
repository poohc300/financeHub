package com.example.financeHub.websocket;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.example.financeHub.kis.KisFuturesPoller;
import com.example.financeHub.kis.KisMarketPoller;
import com.example.financeHub.kis.KisRankingPoller;
import com.example.financeHub.kis.KisWebSocketClient;
import com.example.financeHub.kis.model.KisFuturesPrice;
import com.example.financeHub.kis.model.KisMarketIndex;
import com.example.financeHub.kis.model.KisRankingItem;
import com.example.financeHub.kis.model.KisStockPrice;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

@Component
public class StockPriceWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(StockPriceWebSocketHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    // sessionId → WebSocketSession
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    // isuSrtCd → 구독한 sessionId set
    private final Map<String, Set<String>> symbolSubscriptions = new ConcurrentHashMap<>();
    // sessionId → 구독 중인 isuSrtCd
    private final Map<String, String> sessionSymbol = new ConcurrentHashMap<>();

    private KisWebSocketClient kisWebSocketClient;
    private KisRankingPoller rankingPoller;
    private KisMarketPoller marketPoller;
    private KisFuturesPoller futuresPoller;

    // 순환 의존 방지를 위해 setter 주입
    public void setKisWebSocketClient(KisWebSocketClient kisWebSocketClient) {
        this.kisWebSocketClient = kisWebSocketClient;
    }

    public void setRankingPoller(KisRankingPoller rankingPoller) {
        this.rankingPoller = rankingPoller;
    }

    public void setMarketPoller(KisMarketPoller marketPoller) {
        this.marketPoller = marketPoller;
    }

    public void setFuturesPoller(KisFuturesPoller futuresPoller) {
        this.futuresPoller = futuresPoller;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("프론트 WS 연결: {}", session.getId());
        try {
            // 접속 즉시 캐시된 실시간 가격 전송
            if (kisWebSocketClient != null) {
                for (KisStockPrice price : kisWebSocketClient.getPriceCache().values()) {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(mapper.writeValueAsString(price)));
                    }
                }
            }
            // 접속 즉시 캐시된 랭킹 전송
            if (rankingPoller != null && !rankingPoller.getRankingCache().isEmpty()) {
                String rankingJson = buildRankingMessage(rankingPoller.getRankingCache());
                session.sendMessage(new TextMessage(rankingJson));
            }
            // 접속 즉시 캐시된 시장 지수 전송
            if (marketPoller != null && !marketPoller.getMarketCache().isEmpty()) {
                String marketJson = buildMarketMessage(marketPoller.getMarketCache());
                session.sendMessage(new TextMessage(marketJson));
            }
            // 접속 즉시 캐시된 선물 시세 전송
            if (futuresPoller != null && !futuresPoller.getFuturesCache().isEmpty()) {
                String futuresJson = buildFuturesMessage(futuresPoller.getFuturesCache());
                session.sendMessage(new TextMessage(futuresJson));
            }
        } catch (Exception e) {
            log.error("초기 데이터 전송 실패: {}", e.getMessage());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode json = mapper.readTree(message.getPayload());
            String type = json.path("type").asText();
            String isuSrtCd = json.path("isuSrtCd").asText();

            if ("subscribe".equals(type) && !isuSrtCd.isEmpty()) {
                // 이전 구독 해제
                unsubscribeSession(session.getId());

                // 새 구독 등록
                sessionSymbol.put(session.getId(), isuSrtCd);
                symbolSubscriptions.computeIfAbsent(isuSrtCd, k -> ConcurrentHashMap.newKeySet())
                        .add(session.getId());

                // KIS 구독
                if (kisWebSocketClient != null) {
                    kisWebSocketClient.subscribe(isuSrtCd);
                }
                log.info("구독 등록: {} → {}", session.getId(), isuSrtCd);

            } else if ("unsubscribe".equals(type)) {
                unsubscribeSession(session.getId());
            }
        } catch (Exception e) {
            log.error("메시지 파싱 오류: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        unsubscribeSession(session.getId());
        sessions.remove(session.getId());
        log.info("프론트 WS 종료: {}", session.getId());
    }

    private void unsubscribeSession(String sessionId) {
        String prevSymbol = sessionSymbol.remove(sessionId);
        if (prevSymbol != null) {
            Set<String> subs = symbolSubscriptions.get(prevSymbol);
            if (subs != null) {
                subs.remove(sessionId);
                // 아무도 구독하지 않으면 KIS 구독 해제
                if (subs.isEmpty()) {
                    symbolSubscriptions.remove(prevSymbol);
                    if (kisWebSocketClient != null) {
                        kisWebSocketClient.unsubscribe(prevSymbol);
                    }
                }
            }
        }
    }

    public void broadcastPrice(KisStockPrice price) {
        try {
            String json = mapper.writeValueAsString(price);
            TextMessage msg = new TextMessage(json);
            for (WebSocketSession session : sessions.values()) {
                if (session != null && session.isOpen()) {
                    try {
                        session.sendMessage(msg);
                    } catch (IOException e) {
                        log.error("메시지 전송 실패 sessionId={}: {}", session.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("broadcastPrice 오류: {}", e.getMessage());
        }
    }

    public void broadcastRanking(List<KisRankingItem> items) {
        try {
            String json = buildRankingMessage(items);
            TextMessage msg = new TextMessage(json);
            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(msg);
                    } catch (IOException e) {
                        log.error("랭킹 전송 실패 sessionId={}: {}", session.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("broadcastRanking 오류: {}", e.getMessage());
        }
    }

    public void broadcastMarket(List<KisMarketIndex> items) {
        try {
            String json = buildMarketMessage(items);
            TextMessage msg = new TextMessage(json);
            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(msg);
                    } catch (IOException e) {
                        log.error("시장지수 전송 실패 sessionId={}: {}", session.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("broadcastMarket 오류: {}", e.getMessage());
        }
    }

    public void broadcastFutures(List<KisFuturesPrice> items) {
        try {
            String json = buildFuturesMessage(items);
            TextMessage msg = new TextMessage(json);
            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(msg);
                    } catch (IOException e) {
                        log.error("선물 전송 실패 sessionId={}: {}", session.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("broadcastFutures 오류: {}", e.getMessage());
        }
    }

    private String buildFuturesMessage(List<KisFuturesPrice> items) throws Exception {
        java.util.Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("type", "futures");
        msg.put("items", items);
        return mapper.writeValueAsString(msg);
    }

    private String buildRankingMessage(List<KisRankingItem> items) throws Exception {
        java.util.Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("type", "ranking");
        msg.put("items", items);
        return mapper.writeValueAsString(msg);
    }

    private String buildMarketMessage(List<KisMarketIndex> items) throws Exception {
        java.util.Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("type", "market");
        msg.put("items", items);
        return mapper.writeValueAsString(msg);
    }
}
