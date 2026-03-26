package com.example.financeHub.overseas.fetcher;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.financeHub.kis.KisTokenManager;
import com.example.financeHub.overseas.model.OverseasStockDailyTradingDTO;

@Service
public class OverseasStockFetcher {

    private static final Logger log = LoggerFactory.getLogger(OverseasStockFetcher.class);

    /** 일별 시세 조회 TR_ID */
    private static final String TR_DAILY_PRICE = "HHDFS76240000";

    /**
     * 자동 수집 워치리스트: {거래소코드, 티커, 종목명}
     */
    private static final List<String[]> WATCHLIST = List.of(
        new String[]{"NAS", "AAPL",  "Apple Inc."},
        new String[]{"NAS", "MSFT",  "Microsoft Corp."},
        new String[]{"NAS", "GOOGL", "Alphabet Inc."},
        new String[]{"NAS", "AMZN",  "Amazon.com Inc."},
        new String[]{"NAS", "TSLA",  "Tesla Inc."},
        new String[]{"NAS", "NVDA",  "NVIDIA Corp."},
        new String[]{"NAS", "META",  "Meta Platforms Inc."},
        new String[]{"NAS", "AMD",   "Advanced Micro Devices Inc."},
        new String[]{"NAS", "NFLX",  "Netflix Inc."},
        new String[]{"NYS", "JPM",   "JPMorgan Chase & Co."},
        new String[]{"NYS", "V",     "Visa Inc."},
        new String[]{"NYS", "MA",    "Mastercard Inc."}
    );

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    @Value("${kis.base-url}")
    private String baseUrl;

    private final KisTokenManager tokenManager;
    private final RestTemplate restTemplate;

    public OverseasStockFetcher(KisTokenManager tokenManager, RestTemplate restTemplate) {
        this.tokenManager = tokenManager;
        this.restTemplate = restTemplate;
    }

    /**
     * 워치리스트 전체 최신 1건씩 수집
     * @param bassDt 기준일자 YYYYMMDD (null 시 API 최신일)
     */
    public List<OverseasStockDailyTradingDTO> fetchWatchlist(String bassDt) {
        List<OverseasStockDailyTradingDTO> result = new ArrayList<>();
        for (String[] stock : WATCHLIST) {
            try {
                OverseasStockDailyTradingDTO dto = fetchLatest(stock[0], stock[1], stock[2]);
                if (dto != null) {
                    if (bassDt != null && !bassDt.isEmpty() && !bassDt.equals(dto.getBassDt())) {
                        log.warn("{}/{} 기준일자 불일치: expected={}, actual={}", stock[0], stock[1], bassDt, dto.getBassDt());
                    }
                    result.add(dto);
                }
                Thread.sleep(200); // KIS API rate limit 방지
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("해외주식 수집 실패 - {}/{}: {}", stock[0], stock[1], e.getMessage());
            }
        }
        return result;
    }

    /**
     * 특정 종목 최신 종가 1건 조회
     */
    @SuppressWarnings("unchecked")
    public OverseasStockDailyTradingDTO fetchLatest(String excd, String symb, String name) {
        try {
            List<OverseasStockDailyTradingDTO> history = fetchDailyPriceList(excd, symb, name);
            return history.isEmpty() ? null : history.get(0);
        } catch (Exception e) {
            log.error("해외주식 최신가 조회 실패 - {}/{}: {}", excd, symb, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 특정 종목 일별 시세 목록 조회 (KIS API 기본 최대 100건)
     */
    @SuppressWarnings("unchecked")
    public List<OverseasStockDailyTradingDTO> fetchDailyPriceList(String excd, String symb, String name) {
        List<OverseasStockDailyTradingDTO> result = new ArrayList<>();
        try {
            String token = tokenManager.getAccessToken();
            HttpHeaders headers = buildHeaders(token, TR_DAILY_PRICE);

            String url = baseUrl + "/uapi/overseas-price/v1/quotations/dailyprice"
                + "?AUTH=&EXCD=" + excd + "&SYMB=" + symb + "&GUBN=0&BYMD=&MODP=0";

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) return result;

            Map<String, Object> body = response.getBody();
            if (!"0".equals(body.get("rt_cd"))) {
                log.warn("KIS 해외주식 API 비정상 응답 - {}/{}: rt_cd={}, msg={}",
                    excd, symb, body.get("rt_cd"), body.get("msg1"));
                return result;
            }

            List<Map<String, Object>> output2 = (List<Map<String, Object>>) body.get("output2");
            if (output2 == null) return result;

            for (Map<String, Object> item : output2) {
                String xymd = (String) item.get("xymd");
                if (xymd == null || xymd.isBlank()) continue;

                OverseasStockDailyTradingDTO dto = new OverseasStockDailyTradingDTO();
                dto.setBassDt(xymd);
                dto.setIsuCd(symb);
                dto.setIsuNm(name);
                dto.setExcd(excd);
                dto.setCurr("USD");
                dto.setClsPrc(parseBigDecimal(item.get("clos")));
                dto.setOpnPrc(parseBigDecimal(item.get("open")));
                dto.setHgstPrc(parseBigDecimal(item.get("high")));
                dto.setLwstPrc(parseBigDecimal(item.get("low")));
                dto.setAccTrdVol(parseLong(item.get("tvol")));
                dto.setFlucRt(parseBigDecimal(item.get("rate")));
                result.add(dto);
            }

        } catch (Exception e) {
            log.error("KIS 해외주식 일별시세 조회 실패 - {}/{}: {}", excd, symb, e.getMessage(), e);
        }
        return result;
    }

    private HttpHeaders buildHeaders(String token, String trId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", trId);
        return headers;
    }

    private BigDecimal parseBigDecimal(Object val) {
        if (val == null || val.toString().isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(val.toString().replace(",", ""));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private Long parseLong(Object val) {
        if (val == null || val.toString().isBlank()) return 0L;
        try {
            return Long.parseLong(val.toString().replace(",", ""));
        } catch (Exception e) {
            return 0L;
        }
    }
}
