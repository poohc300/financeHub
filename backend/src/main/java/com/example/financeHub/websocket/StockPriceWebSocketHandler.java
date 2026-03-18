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

import com.example.financeHub.kis.KisWebSocketClient;
import com.example.financeHub.kis.model.KisStockPrice;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    // 순환 의존 방지를 위해 setter 주입
    public void setKisWebSocketClient(KisWebSocketClient kisWebSocketClient) {
        this.kisWebSocketClient = kisWebSocketClient;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("프론트 WS 연결: {}", session.getId());
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
        Set<String> subscriberIds = symbolSubscriptions.get(price.getIsuSrtCd());
        if (subscriberIds == null || subscriberIds.isEmpty()) return;

        try {
            String json = mapper.writeValueAsString(price);
            TextMessage msg = new TextMessage(json);
            for (String sessionId : subscriberIds) {
                WebSocketSession session = sessions.get(sessionId);
                if (session != null && session.isOpen()) {
                    try {
                        session.sendMessage(msg);
                    } catch (IOException e) {
                        log.error("메시지 전송 실패 sessionId={}: {}", sessionId, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("broadcastPrice 오류: {}", e.getMessage());
        }
    }
}
