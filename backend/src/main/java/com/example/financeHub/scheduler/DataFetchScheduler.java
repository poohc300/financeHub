package com.example.financeHub.scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.financeHub.crawler.ipo.IpoCrawler;
import com.example.financeHub.crawler.ipo.IpoDTO;
import com.example.financeHub.crawler.mapper.CrawlerDataMapper;
import com.example.financeHub.crawler.news.NewsCrawler;
import com.example.financeHub.crawler.news.NewsDTO;
import com.example.financeHub.krx.mapper.KrxDataMapper;
import com.example.financeHub.krx.model.GoldMarketDailyTradingDTO;
import com.example.financeHub.krx.model.KosdaqDailyTradingDTO;
import com.example.financeHub.krx.model.KospiDailyTradingDTO;
import com.example.financeHub.krx.model.OilMarketDailyTradingDTO;
import com.example.financeHub.krx.model.StockDailyTradingDTO;
import com.example.financeHub.krx.service.KrxDataService;
import com.example.financeHub.overseas.mapper.OverseasStockMapper;
import com.example.financeHub.overseas.model.OverseasStockDailyTradingDTO;
import com.example.financeHub.overseas.service.OverseasStockService;
import com.example.financeHub.auth.service.AuthService;
import com.example.financeHub.overseas.util.OverseasMarketUtil;
import com.example.financeHub.scheduler.model.SchedulerExecutionLogDTO;

@Component
public class DataFetchScheduler {

    private static final Logger log = LoggerFactory.getLogger(DataFetchScheduler.class);

    private final IpoCrawler ipoCrawlerService;
    private final NewsCrawler newsCrawlerService;
    private final KrxDataService krxDataService;
    private final KrxDataMapper krxDataMapper;
    private final CrawlerDataMapper crawlerDataMapper;
    private final OverseasStockService overseasStockService;
    private final OverseasStockMapper overseasStockMapper;
    private final AuthService authService;

    public DataFetchScheduler(
            IpoCrawler ipoCrawlerService,
            NewsCrawler newsCrawlerService,
            KrxDataService krxDataService,
            KrxDataMapper krxDataMapper,
            CrawlerDataMapper crawlerDataMapper,
            OverseasStockService overseasStockService,
            OverseasStockMapper overseasStockMapper,
            AuthService authService) {
        this.ipoCrawlerService = ipoCrawlerService;
        this.newsCrawlerService = newsCrawlerService;
        this.krxDataService = krxDataService;
        this.krxDataMapper = krxDataMapper;
        this.crawlerDataMapper = crawlerDataMapper;
        this.overseasStockService = overseasStockService;
        this.overseasStockMapper = overseasStockMapper;
        this.authService = authService;
    }

    /** 만료된 Refresh Token 정리 (매일 새벽 3시) */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void cleanupExpiredRefreshTokens() {
        authService.cleanupExpiredTokens();
        log.info("만료된 Refresh Token 정리 완료");
    }

    /** KRX + 크롤링 전체 (평일 19시 — KRX API 당일 종가 공개 후 여유 확보) */
    @Scheduled(cron = "0 0 19 * * MON-FRI", zone = "Asia/Seoul")
    public void fetchAllData() {
        log.info("Starting scheduled data fetch at {}", LocalDateTime.now());

        fetchKospiData();
        fetchKosdaqData();
        fetchGoldMarketData();
        fetchOilMarketData();
        fetchStockData();
        fetchNewsData();
        fetchIpoData();

        log.info("Completed scheduled data fetch at {}", LocalDateTime.now());
    }

    /** 뉴스 매 1시간마다 크롤링 */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void fetchNewsHourly() {
        log.info("Hourly news fetch at {}", LocalDateTime.now());
        fetchNewsData();
    }

    /** 해외주식 워치리스트 수집 (평일 07:00 — 미국 야간장 06:00 종료 후) */
    @Scheduled(cron = "0 0 7 * * MON-FRI", zone = "Asia/Seoul")
    public void fetchOverseasDataScheduled() {
        log.info("Starting overseas stock fetch at {}", LocalDateTime.now());
        fetchOverseasStockData(null);
        log.info("Completed overseas stock fetch at {}", LocalDateTime.now());
    }

    public void fetchKospiData() {
        fetchKospiData(null);
    }

    public FetchResult fetchKospiData(String formattedDate) {
        String jobName = "KOSPI_DAILY_TRADING";
        long startTime = System.currentTimeMillis();
        int processed = 0;
        int inserted = 0;
        int skipped = 0;
        String status = "SUCCESS";
        String errorMessage = null;

        try {
            List<KospiDailyTradingDTO> dataList = formattedDate != null
                    ? krxDataService.getKospiDailyTradingInfo(formattedDate)
                    : krxDataService.getKospiDailyTradingInfo();
            processed = dataList.size();

            if (!dataList.isEmpty()) {
                // Generate hashes
                dataList.forEach(dto -> dto.setDataHash(dto.generateHash()));

                // Find existing hashes
                Set<String> allHashes = dataList.stream()
                        .map(KospiDailyTradingDTO::getDataHash)
                        .collect(Collectors.toSet());
                Set<String> existingHashes = krxDataMapper.findExistingKospiHashes(allHashes);

                // Filter new data
                List<KospiDailyTradingDTO> newData = dataList.stream()
                        .filter(dto -> !existingHashes.contains(dto.getDataHash()))
                        .collect(Collectors.toList());

                inserted = newData.size();
                skipped = processed - inserted;

                if (!newData.isEmpty()) {
                    krxDataMapper.batchInsertKospi(newData);
                }
            }

            log.info("{}: processed={}, inserted={}, skipped={}", jobName, processed, inserted, skipped);
        } catch (Exception e) {
            status = "FAILED";
            errorMessage = e.getMessage();
            log.error("{} failed: {}", jobName, e.getMessage(), e);
        } finally {
            saveExecutionLog(jobName, startTime, status, processed, inserted, skipped, errorMessage);
        }
        return new FetchResult(processed, inserted, skipped, status, errorMessage);
    }

    public void fetchKosdaqData() {
        fetchKosdaqData(null);
    }

    public FetchResult fetchKosdaqData(String formattedDate) {
        String jobName = "KOSDAQ_DAILY_TRADING";
        long startTime = System.currentTimeMillis();
        int processed = 0;
        int inserted = 0;
        int skipped = 0;
        String status = "SUCCESS";
        String errorMessage = null;

        try {
            List<KosdaqDailyTradingDTO> dataList = formattedDate != null
                    ? krxDataService.getKosdaqDailyTradingInfo(formattedDate)
                    : krxDataService.getKosdaqDailyTradingInfo();
            processed = dataList.size();

            if (!dataList.isEmpty()) {
                dataList.forEach(dto -> dto.setDataHash(dto.generateHash()));

                Set<String> allHashes = dataList.stream()
                        .map(KosdaqDailyTradingDTO::getDataHash)
                        .collect(Collectors.toSet());
                Set<String> existingHashes = krxDataMapper.findExistingKosdaqHashes(allHashes);

                List<KosdaqDailyTradingDTO> newData = dataList.stream()
                        .filter(dto -> !existingHashes.contains(dto.getDataHash()))
                        .collect(Collectors.toList());

                inserted = newData.size();
                skipped = processed - inserted;

                if (!newData.isEmpty()) {
                    krxDataMapper.batchInsertKosdaq(newData);
                }
            }

            log.info("{}: processed={}, inserted={}, skipped={}", jobName, processed, inserted, skipped);
        } catch (Exception e) {
            status = "FAILED";
            errorMessage = e.getMessage();
            log.error("{} failed: {}", jobName, e.getMessage(), e);
        } finally {
            saveExecutionLog(jobName, startTime, status, processed, inserted, skipped, errorMessage);
        }
        return new FetchResult(processed, inserted, skipped, status, errorMessage);
    }

    public void fetchGoldMarketData() {
        fetchGoldMarketData(null);
    }

    public FetchResult fetchGoldMarketData(String formattedDate) {
        String jobName = "GOLD_MARKET_DAILY_TRADING";
        long startTime = System.currentTimeMillis();
        int processed = 0;
        int inserted = 0;
        int skipped = 0;
        String status = "SUCCESS";
        String errorMessage = null;

        try {
            List<GoldMarketDailyTradingDTO> dataList = formattedDate != null
                    ? krxDataService.getGoldMarketDailyTradingInfo(formattedDate)
                    : krxDataService.getGoldMarketDailyTradingInfo();
            processed = dataList.size();

            if (!dataList.isEmpty()) {
                dataList.forEach(dto -> dto.setDataHash(dto.generateHash()));

                Set<String> allHashes = dataList.stream()
                        .map(GoldMarketDailyTradingDTO::getDataHash)
                        .collect(Collectors.toSet());
                Set<String> existingHashes = krxDataMapper.findExistingGoldMarketHashes(allHashes);

                List<GoldMarketDailyTradingDTO> newData = dataList.stream()
                        .filter(dto -> !existingHashes.contains(dto.getDataHash()))
                        .collect(Collectors.toList());

                inserted = newData.size();
                skipped = processed - inserted;

                if (!newData.isEmpty()) {
                    krxDataMapper.batchInsertGoldMarket(newData);
                }
            }

            log.info("{}: processed={}, inserted={}, skipped={}", jobName, processed, inserted, skipped);
        } catch (Exception e) {
            status = "FAILED";
            errorMessage = e.getMessage();
            log.error("{} failed: {}", jobName, e.getMessage(), e);
        } finally {
            saveExecutionLog(jobName, startTime, status, processed, inserted, skipped, errorMessage);
        }
        return new FetchResult(processed, inserted, skipped, status, errorMessage);
    }

    public void fetchOilMarketData() {
        fetchOilMarketData(null);
    }

    public FetchResult fetchOilMarketData(String formattedDate) {
        String jobName = "OIL_MARKET_DAILY_TRADING";
        long startTime = System.currentTimeMillis();
        int processed = 0;
        int inserted = 0;
        int skipped = 0;
        String status = "SUCCESS";
        String errorMessage = null;

        try {
            List<OilMarketDailyTradingDTO> dataList = formattedDate != null
                    ? krxDataService.getOilMarketDailyTradingInfo(formattedDate)
                    : krxDataService.getOilMarketDailyTradingInfo();
            processed = dataList.size();

            if (!dataList.isEmpty()) {
                dataList.forEach(dto -> dto.setDataHash(dto.generateHash()));

                Set<String> allHashes = dataList.stream()
                        .map(OilMarketDailyTradingDTO::getDataHash)
                        .collect(Collectors.toSet());
                Set<String> existingHashes = krxDataMapper.findExistingOilMarketHashes(allHashes);

                List<OilMarketDailyTradingDTO> newData = dataList.stream()
                        .filter(dto -> !existingHashes.contains(dto.getDataHash()))
                        .collect(Collectors.toList());

                inserted = newData.size();
                skipped = processed - inserted;

                if (!newData.isEmpty()) {
                    krxDataMapper.batchInsertOilMarket(newData);
                }
            }

            log.info("{}: processed={}, inserted={}, skipped={}", jobName, processed, inserted, skipped);
        } catch (Exception e) {
            status = "FAILED";
            errorMessage = e.getMessage();
            log.error("{} failed: {}", jobName, e.getMessage(), e);
        } finally {
            saveExecutionLog(jobName, startTime, status, processed, inserted, skipped, errorMessage);
        }
        return new FetchResult(processed, inserted, skipped, status, errorMessage);
    }

    public record FetchResult(int processed, int inserted, int skipped, String status, String errorMessage) {}

    public FetchResult fetchOverseasStockData() {
        return fetchOverseasStockData(null);
    }

    public FetchResult fetchOverseasStockData(String formattedDate) {
        String jobName = "OVERSEAS_STOCK_DAILY_TRADING";
        long startTime = System.currentTimeMillis();
        int processed = 0;
        int inserted = 0;
        int skipped = 0;
        String status = "SUCCESS";
        String errorMessage = null;

        try {
            String targetDate = (formattedDate != null) ? formattedDate : OverseasMarketUtil.getCollectionDate();
            List<OverseasStockDailyTradingDTO> dataList = overseasStockService.getWatchlistData(targetDate);
            processed = dataList.size();

            if (!dataList.isEmpty()) {
                dataList.forEach(dto -> dto.setDataHash(dto.generateHash()));

                Set<String> allHashes = dataList.stream()
                        .map(OverseasStockDailyTradingDTO::getDataHash)
                        .collect(Collectors.toSet());
                Set<String> existingHashes = overseasStockMapper.findExistingHashes(allHashes);

                List<OverseasStockDailyTradingDTO> newData = dataList.stream()
                        .filter(dto -> !existingHashes.contains(dto.getDataHash()))
                        .collect(Collectors.toList());

                inserted = newData.size();
                skipped = processed - inserted;

                if (!newData.isEmpty()) {
                    overseasStockMapper.batchInsert(newData);
                }
            }

            log.info("{}: processed={}, inserted={}, skipped={}", jobName, processed, inserted, skipped);
        } catch (Exception e) {
            status = "FAILED";
            errorMessage = e.getMessage();
            log.error("{} failed: {}", jobName, e.getMessage(), e);
        } finally {
            saveExecutionLog(jobName, startTime, status, processed, inserted, skipped, errorMessage);
        }
        return new FetchResult(processed, inserted, skipped, status, errorMessage);
    }

    public void fetchStockData() {
        fetchStockData(null);
    }

    public FetchResult fetchStockData(String formattedDate) {
        String jobName = "STOCK_DAILY_TRADING";
        long startTime = System.currentTimeMillis();
        int processed = 0;
        int inserted = 0;
        int skipped = 0;
        String status = "SUCCESS";
        String errorMessage = null;

        try {
            List<StockDailyTradingDTO> dataList = formattedDate != null
                    ? krxDataService.getStockDailyTradingInfo(formattedDate)
                    : krxDataService.getStockDailyTradingInfo();
            processed = dataList.size();

            if (!dataList.isEmpty()) {
                dataList.forEach(dto -> dto.setDataHash(dto.generateHash()));

                Set<String> allHashes = dataList.stream()
                        .map(StockDailyTradingDTO::getDataHash)
                        .collect(Collectors.toSet());
                Set<String> existingHashes = krxDataMapper.findExistingStockHashes(allHashes);

                List<StockDailyTradingDTO> newData = dataList.stream()
                        .filter(dto -> !existingHashes.contains(dto.getDataHash()))
                        .collect(Collectors.toList());

                inserted = newData.size();
                skipped = processed - inserted;

                if (!newData.isEmpty()) {
                    krxDataMapper.batchInsertStock(newData);
                }
            }

            log.info("{}: processed={}, inserted={}, skipped={}", jobName, processed, inserted, skipped);
        } catch (Exception e) {
            status = "FAILED";
            errorMessage = e.getMessage();
            log.error("{} failed: {}", jobName, e.getMessage(), e);
        } finally {
            saveExecutionLog(jobName, startTime, status, processed, inserted, skipped, errorMessage);
        }
        return new FetchResult(processed, inserted, skipped, status, errorMessage);
    }

    public void fetchNewsData() {
        String jobName = "NEWS";
        long startTime = System.currentTimeMillis();
        int processed = 0;
        int inserted = 0;
        int skipped = 0;
        String status = "SUCCESS";
        String errorMessage = null;

        try {
            List<NewsDTO> dataList = newsCrawlerService.getTodayNews();
            processed = dataList.size();

            if (!dataList.isEmpty()) {
                String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
                dataList.forEach(dto -> {
                    dto.setDataHash(dto.generateHash());
                    dto.setPublishedAt(today);
                });

                Set<String> allHashes = dataList.stream()
                        .map(NewsDTO::getDataHash)
                        .collect(Collectors.toSet());
                Set<String> existingHashes = crawlerDataMapper.findExistingNewsHashes(allHashes);

                List<NewsDTO> newData = dataList.stream()
                        .filter(dto -> !existingHashes.contains(dto.getDataHash()))
                        .collect(Collectors.toList());

                inserted = newData.size();
                skipped = processed - inserted;

                if (!newData.isEmpty()) {
                    crawlerDataMapper.batchInsertNews(newData);
                }
            }

            log.info("{}: processed={}, inserted={}, skipped={}", jobName, processed, inserted, skipped);
        } catch (Exception e) {
            status = "FAILED";
            errorMessage = e.getMessage();
            log.error("{} failed: {}", jobName, e.getMessage(), e);
        } finally {
            saveExecutionLog(jobName, startTime, status, processed, inserted, skipped, errorMessage);
        }
    }

    public void fetchIpoData() {
        String jobName = "IPO";
        long startTime = System.currentTimeMillis();
        int processed = 0;
        int inserted = 0;
        int skipped = 0;
        String status = "SUCCESS";
        String errorMessage = null;

        try {
            List<IpoDTO> dataList = ipoCrawlerService.getTodayIpoList();
            processed = dataList.size();

            if (!dataList.isEmpty()) {
                dataList.forEach(dto -> dto.setDataHash(dto.generateHash()));

                Set<String> allHashes = dataList.stream()
                        .map(IpoDTO::getDataHash)
                        .collect(Collectors.toSet());
                Set<String> existingHashes = crawlerDataMapper.findExistingIpoHashes(allHashes);

                List<IpoDTO> newData = dataList.stream()
                        .filter(dto -> !existingHashes.contains(dto.getDataHash()))
                        .collect(Collectors.toList());

                inserted = newData.size();
                skipped = processed - inserted;

                if (!newData.isEmpty()) {
                    crawlerDataMapper.batchInsertIpo(newData);
                }
            }

            log.info("{}: processed={}, inserted={}, skipped={}", jobName, processed, inserted, skipped);
        } catch (Exception e) {
            status = "FAILED";
            errorMessage = e.getMessage();
            log.error("{} failed: {}", jobName, e.getMessage(), e);
        } finally {
            saveExecutionLog(jobName, startTime, status, processed, inserted, skipped, errorMessage);
        }
    }

    private void saveExecutionLog(String jobName, long startTime, String status,
                                   int processed, int inserted, int skipped, String errorMessage) {
        try {
            long duration = System.currentTimeMillis() - startTime;
            SchedulerExecutionLogDTO logDTO = SchedulerExecutionLogDTO.builder()
                    .jobName(jobName)
                    .executionTime(LocalDateTime.now())
                    .status(status)
                    .recordsProcessed(processed)
                    .recordsInserted(inserted)
                    .recordsSkipped(skipped)
                    .errorMessage(errorMessage)
                    .executionDurationMs(duration)
                    .build();
            crawlerDataMapper.insertExecutionLog(logDTO);
        } catch (Exception e) {
            log.error("Failed to save execution log for {}: {}", jobName, e.getMessage(), e);
        }
    }
}
