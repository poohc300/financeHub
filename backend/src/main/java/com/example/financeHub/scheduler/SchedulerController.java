package com.example.financeHub.scheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.financeHub.scheduler.DataFetchScheduler.FetchResult;

import com.example.financeHub.crawler.mapper.CrawlerDataMapper;
import com.example.financeHub.scheduler.model.SchedulerExecutionLogDTO;

@RestController
@RequestMapping("/admin")
public class SchedulerController {

    private final DataFetchScheduler scheduler;
    private final CrawlerDataMapper crawlerDataMapper;

    public SchedulerController(DataFetchScheduler scheduler, CrawlerDataMapper crawlerDataMapper) {
        this.scheduler = scheduler;
        this.crawlerDataMapper = crawlerDataMapper;
    }

    /**
     * KRX 데이터만 수동 수집 (KOSPI, KOSDAQ, 금, 오일, 주식)
     * Selenium 없이 동작하므로 서버에서 안전하게 실행 가능
     */
    @PostMapping("/run-krx")
    public ResponseEntity<Map<String, String>> runKrx() {
        scheduler.fetchKospiData();
        scheduler.fetchKosdaqData();
        scheduler.fetchGoldMarketData();
        scheduler.fetchOilMarketData();
        scheduler.fetchStockData();
        return ResponseEntity.ok(Map.of("status", "KRX 데이터 수집 완료"));
    }

    /**
     * 전체 스케줄러 수동 실행 (KRX + 뉴스 + IPO 크롤링 포함)
     * 서버에 Chromium이 없으면 뉴스/IPO 크롤링은 실패할 수 있음
     */
    @PostMapping("/run-all")
    public ResponseEntity<Map<String, String>> runAll() {
        scheduler.fetchAllData();
        return ResponseEntity.ok(Map.of("status", "전체 데이터 수집 완료"));
    }

    /**
     * 특정 날짜 KRX 전체 수집 (KOSPI, KOSDAQ, 금, 유가) — YYYYMMDD
     */
    @PostMapping("/run-krx-date")
    public ResponseEntity<Map<String, Object>> runKrxByDate(@RequestParam String date) {
        FetchResult kospi  = scheduler.fetchKospiData(date);
        FetchResult kosdaq = scheduler.fetchKosdaqData(date);
        FetchResult gold   = scheduler.fetchGoldMarketData(date);
        FetchResult oil    = scheduler.fetchOilMarketData(date);
        FetchResult stock  = scheduler.fetchStockData(date);

        Map<String, Object> response = new HashMap<>();
        response.put("date", date);
        response.put("kospi",  toMap("KOSPI",  date, kospi));
        response.put("kosdaq", toMap("KOSDAQ", date, kosdaq));
        response.put("gold",   toMap("금",     date, gold));
        response.put("oil",    toMap("유가",   date, oil));
        response.put("stock",  toMap("종목",   date, stock));
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 날짜 금 시세 수집 (YYYYMMDD)
     */
    @PostMapping("/run-gold")
    public ResponseEntity<Map<String, Object>> runGold(@RequestParam String date) {
        FetchResult result = scheduler.fetchGoldMarketData(date);
        return ResponseEntity.ok(toMap("금", date, result));
    }

    /**
     * 특정 날짜 유가 수집 (YYYYMMDD)
     */
    @PostMapping("/run-oil")
    public ResponseEntity<Map<String, Object>> runOil(@RequestParam String date) {
        FetchResult result = scheduler.fetchOilMarketData(date);
        return ResponseEntity.ok(toMap("유가", date, result));
    }

    /**
     * 특정 날짜 금 + 유가 동시 수집 (YYYYMMDD)
     */
    @PostMapping("/run-commodity")
    public ResponseEntity<Map<String, Object>> runCommodity(@RequestParam String date) {
        FetchResult gold = scheduler.fetchGoldMarketData(date);
        FetchResult oil = scheduler.fetchOilMarketData(date);
        Map<String, Object> response = new HashMap<>();
        response.put("date", date);
        response.put("gold", toMap("금", date, gold));
        response.put("oil", toMap("유가", date, oil));
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> toMap(String label, String date, FetchResult r) {
        Map<String, Object> map = new HashMap<>();
        map.put("label", label);
        map.put("date", date);
        map.put("status", r.status());
        map.put("processed", r.processed());
        map.put("inserted", r.inserted());
        map.put("skipped", r.skipped());
        if (r.errorMessage() != null) map.put("error", r.errorMessage());
        return map;
    }

    /**
     * 해외주식 워치리스트 수동 수집
     * date: YYYYMMDD (생략 시 전날 기준)
     */
    @PostMapping("/run-overseas")
    public ResponseEntity<Map<String, Object>> runOverseas(
            @RequestParam(required = false) String date) {
        FetchResult result = scheduler.fetchOverseasStockData(date);
        return ResponseEntity.ok(toMap("해외주식", date != null ? date : "최신", result));
    }

    /**
     * 스케줄러 실행 로그 조회
     */
    @GetMapping("/scheduler-logs")
    public ResponseEntity<List<SchedulerExecutionLogDTO>> getLogs(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(crawlerDataMapper.selectRecentLogs(limit));
    }
}
