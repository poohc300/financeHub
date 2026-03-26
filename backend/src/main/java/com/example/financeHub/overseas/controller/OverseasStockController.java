package com.example.financeHub.overseas.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.financeHub.overseas.fetcher.OverseasStockFetcher;
import com.example.financeHub.overseas.mapper.OverseasStockMapper;
import com.example.financeHub.overseas.model.OverseasStockDailyTradingDTO;
import com.example.financeHub.overseas.util.OverseasMarketUtil;

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
     * 종목명/티커 검색 (DB 기준)
     * GET /overseas/search?keyword=AAPL
     */
    @GetMapping("/search")
    public ResponseEntity<List<OverseasStockDailyTradingDTO>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(overseasStockMapper.searchStocks(keyword, limit));
    }

    /**
     * 차트 데이터 조회 (DB 이력)
     * GET /overseas/chart?excd=NAS&symb=AAPL&period=1M
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

        return ResponseEntity.ok(data);
    }

    /**
     * 실시간 현재가 조회 (KIS API 직접 호출)
     * GET /overseas/price?excd=NAS&symb=AAPL
     */
    @GetMapping("/price")
    public ResponseEntity<?> price(
            @RequestParam String excd,
            @RequestParam String symb,
            @RequestParam(defaultValue = "") String name) {
        try {
            OverseasStockDailyTradingDTO dto = overseasStockFetcher.fetchLatest(
                excd.toUpperCase(), symb.toUpperCase(), name);
            if (dto == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("해외주식 실시간 조회 실패 - {}/{}: {}", excd, symb, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
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
}
