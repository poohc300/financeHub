package com.example.financeHub.overseas.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.financeHub.overseas.fetcher.OverseasStockFetcher;
import com.example.financeHub.overseas.mapper.OverseasStockMapper;
import com.example.financeHub.overseas.model.Overseas52WeekDTO;
import com.example.financeHub.overseas.model.OverseasCurrentPriceDTO;
import com.example.financeHub.overseas.model.OverseasStockDailyTradingDTO;

@RestController
@RequestMapping("/overseas")
public class OverseasStockController {

    private static final Logger log = LoggerFactory.getLogger(OverseasStockController.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OverseasStockMapper overseasStockMapper;
    private final OverseasStockFetcher overseasStockFetcher;

    public OverseasStockController(OverseasStockMapper overseasStockMapper,
                                   OverseasStockFetcher overseasStockFetcher) {
        this.overseasStockMapper = overseasStockMapper;
        this.overseasStockFetcher = overseasStockFetcher;
    }

    /**
     * 종목명/티커 검색
     * 1순위: DB 수집 종목에서 검색
     * 2순위: DB에 없으면 KIS API로 직접 조회 (NAS → NYS → AMS 순 시도)
     * GET /overseas/search?keyword=PLTR
     */
    @GetMapping("/search")
    public ResponseEntity<List<OverseasStockDailyTradingDTO>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") int limit) {

        List<OverseasStockDailyTradingDTO> dbResults = overseasStockMapper.searchStocks(keyword, limit);
        if (!dbResults.isEmpty()) return ResponseEntity.ok(dbResults);

        // DB에 없는 경우 티커로 판단되면 KIS API 직접 조회
        String kw = keyword.trim().toUpperCase();
        if (kw.length() <= 6 && kw.matches("[A-Z0-9.]+")) {
            try {
                OverseasCurrentPriceDTO live = overseasStockFetcher.lookupByTicker(kw);
                if (live != null) {
                    return ResponseEntity.ok(List.of(toDaily(live)));
                }
            } catch (Exception e) {
                log.warn("KIS 직접 조회 실패 - {}: {}", kw, e.getMessage());
            }
        }
        return ResponseEntity.ok(List.of());
    }

    /**
     * 차트 데이터 조회
     * 1순위: DB 이력
     * 2순위: DB에 없으면 KIS API 직접 조회 (저장 없이 반환)
     * GET /overseas/chart?excd=NYS&symb=PLTR&period=3M
     * period: 1W, 1M, 3M, 6M, 1Y
     */
    @GetMapping("/chart")
    public ResponseEntity<List<OverseasStockDailyTradingDTO>> chart(
            @RequestParam String excd,
            @RequestParam String symb,
            @RequestParam(defaultValue = "3M") String period) {

        String endDate = LocalDate.now().format(DATE_FMT);
        String startDate = calcStartDate(period);

        List<OverseasStockDailyTradingDTO> data = overseasStockMapper.selectHistoryByDateRange(
            symb.toUpperCase(), excd.toUpperCase(), startDate, endDate);

        if (data.isEmpty()) {
            log.info("DB 데이터 없음, KIS API 직접 조회: {}/{}", excd, symb);
            List<OverseasStockDailyTradingDTO> live = overseasStockFetcher.fetchDailyPriceList(
                excd.toUpperCase(), symb.toUpperCase(), symb.toUpperCase());
            data = live.stream()
                .filter(d -> d.getBassDt() != null
                          && d.getBassDt().compareTo(startDate) >= 0
                          && d.getBassDt().compareTo(endDate) <= 0)
                .collect(Collectors.toList());
        }

        return ResponseEntity.ok(data);
    }

    /**
     * 현재가 조회 — HHDFS00000300 (환율 t_xrat 포함)
     * GET /overseas/price?excd=NAS&symb=AAPL&name=Apple
     */
    @GetMapping("/price")
    public ResponseEntity<?> price(
            @RequestParam String excd,
            @RequestParam String symb,
            @RequestParam(defaultValue = "") String name) {
        try {
            OverseasCurrentPriceDTO dto = overseasStockFetcher.fetchCurrentPrice(
                excd.toUpperCase(), symb.toUpperCase(), name);
            if (dto == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("해외주식 현재가 조회 실패 - {}/{}: {}", excd, symb, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 52주 신고가/저가 대비 현재가 통계
     * GET /overseas/52week
     */
    @GetMapping("/52week")
    public ResponseEntity<List<Overseas52WeekDTO>> weekStats52() {
        List<Overseas52WeekDTO> result = overseasStockMapper.select52WeekStats();
        result.forEach(Overseas52WeekDTO::calcPct);
        return ResponseEntity.ok(result);
    }

    private String calcStartDate(String period) {
        LocalDate today = LocalDate.now();
        return switch (period) {
            case "1W" -> today.minusWeeks(1).format(DATE_FMT);
            case "1M" -> today.minusMonths(1).format(DATE_FMT);
            case "6M" -> today.minusMonths(6).format(DATE_FMT);
            case "1Y" -> today.minusYears(1).format(DATE_FMT);
            default   -> today.minusMonths(3).format(DATE_FMT); // 3M
        };
    }

    /** OverseasCurrentPriceDTO → OverseasStockDailyTradingDTO 변환 (검색 fallback용) */
    private OverseasStockDailyTradingDTO toDaily(OverseasCurrentPriceDTO live) {
        OverseasStockDailyTradingDTO dto = new OverseasStockDailyTradingDTO();
        dto.setIsuCd(live.getIsuCd());
        dto.setIsuNm(live.getIsuNm() != null && !live.getIsuNm().isEmpty()
                     ? live.getIsuNm() : live.getIsuCd());
        dto.setExcd(live.getExcd());
        dto.setCurr("USD");
        dto.setClsPrc(live.getLastPrc());
        dto.setFlucRt(live.getRate());
        dto.setBassDt(LocalDate.now().format(DATE_FMT));
        return dto;
    }
}
