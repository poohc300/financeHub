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
import com.example.financeHub.overseas.model.OverseasCurrentPriceDTO;
import com.example.financeHub.overseas.model.OverseasStockDailyTradingDTO;
import com.example.financeHub.overseas.service.ExchangeRateService;

@Service
public class OverseasStockFetcher {

    private static final Logger log = LoggerFactory.getLogger(OverseasStockFetcher.class);

    /** 일별 시세 조회 TR_ID */
    private static final String TR_DAILY_PRICE    = "HHDFS76240000";
    /** 현재가 조회 TR_ID (환율 포함) */
    private static final String TR_CURRENT_PRICE  = "HHDFS00000300";

    /**
     * 자동 수집 워치리스트: {거래소코드, 티커, 종목명}
     * 30종목 (NAS 15 + NYS 15)
     */
    private static final List<String[]> WATCHLIST = List.of(
        // ── NASDAQ ──────────────────────────────────
        new String[]{"NAS", "AAPL",  "Apple Inc."},
        new String[]{"NAS", "MSFT",  "Microsoft Corp."},
        new String[]{"NAS", "GOOGL", "Alphabet Inc."},
        new String[]{"NAS", "AMZN",  "Amazon.com Inc."},
        new String[]{"NAS", "NVDA",  "NVIDIA Corp."},
        new String[]{"NAS", "META",  "Meta Platforms Inc."},
        new String[]{"NAS", "TSLA",  "Tesla Inc."},
        new String[]{"NAS", "AMD",   "Advanced Micro Devices Inc."},
        new String[]{"NAS", "INTC",  "Intel Corp."},
        new String[]{"NAS", "QCOM",  "Qualcomm Inc."},
        new String[]{"NAS", "AVGO",  "Broadcom Inc."},
        new String[]{"NAS", "NFLX",  "Netflix Inc."},
        new String[]{"NAS", "ORCL",  "Oracle Corp."},
        new String[]{"NAS", "ADBE",  "Adobe Inc."},
        new String[]{"NAS", "UBER",  "Uber Technologies Inc."},
        // ── NYSE ────────────────────────────────────
        new String[]{"NYS", "JPM",   "JPMorgan Chase & Co."},
        new String[]{"NYS", "V",     "Visa Inc."},
        new String[]{"NYS", "MA",    "Mastercard Inc."},
        new String[]{"NYS", "BAC",   "Bank of America Corp."},
        new String[]{"NYS", "GS",    "Goldman Sachs Group Inc."},
        new String[]{"NYS", "JNJ",   "Johnson & Johnson"},
        new String[]{"NYS", "UNH",   "UnitedHealth Group Inc."},
        new String[]{"NYS", "PFE",   "Pfizer Inc."},
        new String[]{"NYS", "KO",    "Coca-Cola Co."},
        new String[]{"NYS", "WMT",   "Walmart Inc."},
        new String[]{"NYS", "HD",    "Home Depot Inc."},
        new String[]{"NYS", "NKE",   "Nike Inc."},
        new String[]{"NYS", "XOM",   "Exxon Mobil Corp."},
        new String[]{"NYS", "CVX",   "Chevron Corp."},
        new String[]{"NYS", "DIS",   "Walt Disney Co."}
    );

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    @Value("${kis.base-url}")
    private String baseUrl;

    private final KisTokenManager tokenManager;
    private final RestTemplate restTemplate;
    private final ExchangeRateService exchangeRateService;

    public OverseasStockFetcher(KisTokenManager tokenManager, RestTemplate restTemplate,
                                 ExchangeRateService exchangeRateService) {
        this.tokenManager = tokenManager;
        this.restTemplate = restTemplate;
        this.exchangeRateService = exchangeRateService;
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

    /**
     * HHDFS00000300 — 해외주식 현재가 조회 (환율 포함)
     * output.t_xrat = 환율, output.last = 현재가(USD)
     */
    @SuppressWarnings("unchecked")
    public OverseasCurrentPriceDTO fetchCurrentPrice(String excd, String symb, String name) {
        try {
            String token = tokenManager.getAccessToken();
            HttpHeaders headers = buildHeaders(token, TR_CURRENT_PRICE);

            String url = baseUrl + "/uapi/overseas-price/v1/quotations/price"
                + "?AUTH=&EXCD=" + excd + "&SYMB=" + symb;

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) return null;

            Map<String, Object> body = response.getBody();
            if (!"0".equals(body.get("rt_cd"))) {
                log.warn("KIS 현재가 API 비정상 응답 - {}/{}: rt_cd={}, msg={}",
                    excd, symb, body.get("rt_cd"), body.get("msg1"));
                return null;
            }

            Map<String, Object> output = (Map<String, Object>) body.get("output");
            if (output == null) return null;

            OverseasCurrentPriceDTO dto = new OverseasCurrentPriceDTO();
            dto.setIsuCd(symb);
            dto.setIsuNm(name);
            dto.setExcd(excd);
            dto.setCurr("USD");
            dto.setLastPrc(parseBigDecimal(output.get("last")));
            dto.setBasePrc(parseBigDecimal(output.get("base")));
            dto.setDiff(parseBigDecimal(output.get("diff")));
            dto.setRate(parseBigDecimal(output.get("rate")));
            dto.setSign((String) output.get("sign"));
            dto.setTvol(parseLong(output.get("tvol")));

            // 환율: KIS 응답에 없으므로 ExchangeRateService (frankfurter.app ECB 기준) 사용
            dto.setXrat(exchangeRateService.getUsdKrw());
            dto.calcKrwPrice();

            log.info("KIS 현재가 조회 - {}/{}: ${}, 환율={}, ₩{}",
                excd, symb, dto.getLastPrc(), dto.getXrat(), dto.getKrwPrc());
            return dto;

        } catch (Exception e) {
            log.error("KIS 현재가 조회 실패 - {}/{}: {}", excd, symb, e.getMessage(), e);
            return null;
        }
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
