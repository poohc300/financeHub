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
import com.example.financeHub.krx.service.KrxDataService;
import com.example.financeHub.scheduler.model.SchedulerExecutionLogDTO;

@Component
public class DataFetchScheduler {

    private static final Logger log = LoggerFactory.getLogger(DataFetchScheduler.class);

    private final IpoCrawler ipoCrawlerService;
    private final NewsCrawler newsCrawlerService;
    private final KrxDataService krxDataService;
    private final KrxDataMapper krxDataMapper;
    private final CrawlerDataMapper crawlerDataMapper;

    public DataFetchScheduler(
            IpoCrawler ipoCrawlerService,
            NewsCrawler newsCrawlerService,
            KrxDataService krxDataService,
            KrxDataMapper krxDataMapper,
            CrawlerDataMapper crawlerDataMapper) {
        this.ipoCrawlerService = ipoCrawlerService;
        this.newsCrawlerService = newsCrawlerService;
        this.krxDataService = krxDataService;
        this.krxDataMapper = krxDataMapper;
        this.crawlerDataMapper = crawlerDataMapper;
    }

    @Scheduled(cron = "0 0 18 * * MON-FRI", zone = "Asia/Seoul")
    public void fetchAllData() {
        log.info("Starting scheduled data fetch at {}", LocalDateTime.now());

        fetchKospiData();
        fetchKosdaqData();
        fetchGoldMarketData();
        fetchOilMarketData();
        fetchNewsData();
        fetchIpoData();

        log.info("Completed scheduled data fetch at {}", LocalDateTime.now());
    }

    public void fetchKospiData() {
        String jobName = "KOSPI_DAILY_TRADING";
        long startTime = System.currentTimeMillis();
        int processed = 0;
        int inserted = 0;
        int skipped = 0;
        String status = "SUCCESS";
        String errorMessage = null;

        try {
            List<KospiDailyTradingDTO> dataList = krxDataService.getKospiDailyTradingInfo();
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
    }

    public void fetchKosdaqData() {
        String jobName = "KOSDAQ_DAILY_TRADING";
        long startTime = System.currentTimeMillis();
        int processed = 0;
        int inserted = 0;
        int skipped = 0;
        String status = "SUCCESS";
        String errorMessage = null;

        try {
            List<KosdaqDailyTradingDTO> dataList = krxDataService.getKosdaqDailyTradingInfo();
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
    }

    public void fetchGoldMarketData() {
        String jobName = "GOLD_MARKET_DAILY_TRADING";
        long startTime = System.currentTimeMillis();
        int processed = 0;
        int inserted = 0;
        int skipped = 0;
        String status = "SUCCESS";
        String errorMessage = null;

        try {
            List<GoldMarketDailyTradingDTO> dataList = krxDataService.getGoldMarketDailyTradingInfo();
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
    }

    public void fetchOilMarketData() {
        String jobName = "OIL_MARKET_DAILY_TRADING";
        long startTime = System.currentTimeMillis();
        int processed = 0;
        int inserted = 0;
        int skipped = 0;
        String status = "SUCCESS";
        String errorMessage = null;

        try {
            List<OilMarketDailyTradingDTO> dataList = krxDataService.getOilMarketDailyTradingInfo();
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
