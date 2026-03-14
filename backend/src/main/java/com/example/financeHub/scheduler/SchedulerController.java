package com.example.financeHub.scheduler;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
     * 스케줄러 실행 로그 조회
     */
    @GetMapping("/scheduler-logs")
    public ResponseEntity<List<SchedulerExecutionLogDTO>> getLogs(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(crawlerDataMapper.selectRecentLogs(limit));
    }
}
