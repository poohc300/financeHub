package com.example.financeHub.dashboard.controller;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.financeHub.crawler.ipo.IpoDTO;
import com.example.financeHub.crawler.ipo.IpoCrawler;
import com.example.financeHub.crawler.mapper.CrawlerDataMapper;
import com.example.financeHub.crawler.news.NewsDTO;
import com.example.financeHub.crawler.news.NewsCrawler;
import com.example.financeHub.dashboard.model.ChartDataDTO;
import com.example.financeHub.dashboard.model.DashboardDTO;
import com.example.financeHub.krx.mapper.KrxDataMapper;
import com.example.financeHub.krx.model.GoldMarketDailyTradingDTO;
import com.example.financeHub.krx.model.KosdaqDailyTradingDTO;
import com.example.financeHub.krx.model.KospiDailyTradingDTO;
import com.example.financeHub.krx.model.OilMarketDailyTradingDTO;
import com.example.financeHub.krx.model.StockDailyTradingDTO;
import com.example.financeHub.krx.service.KrxDataService;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {


    private final NewsCrawler newsCrawlerService;
    private final IpoCrawler ipoCrawlerService;
    private final KrxDataService krxDataService;
    private final KrxDataMapper krxDataMapper;
    private final CrawlerDataMapper crawlerDataMapper;

    public DashboardController(
	    NewsCrawler newsCrawlerService,
	    IpoCrawler ipoCrawlerService,
	    KrxDataService krxDataService,
	    KrxDataMapper krxDataMapper,
	    CrawlerDataMapper crawlerDataMapper
	    ) {
	this.newsCrawlerService = newsCrawlerService;
	this.ipoCrawlerService = ipoCrawlerService;
	this.krxDataService = krxDataService;
	this.krxDataMapper = krxDataMapper;
	this.crawlerDataMapper = crawlerDataMapper;
    }



    @GetMapping("/data")
    public ResponseEntity getData() {
        List<NewsDTO> todayNews = crawlerDataMapper.selectLatestNews();
        List<IpoDTO> todayIpoList = crawlerDataMapper.selectLatestIpo();
        List<GoldMarketDailyTradingDTO> latestGoldMarket = krxDataMapper.selectLatestGoldMarket();
        List<OilMarketDailyTradingDTO> latestOilMarket = krxDataMapper.selectLatestOilMarket();
        List<KospiDailyTradingDTO> latestKospiMarket = krxDataMapper.selectLatestKospi();
        List<KosdaqDailyTradingDTO> latestKosdaqMarket = krxDataMapper.selectLatestKosdaq();
        List<StockDailyTradingDTO> topGainers = krxDataMapper.selectTopGainers(5);
        List<StockDailyTradingDTO> topVolume = krxDataMapper.selectTopVolume(5);

        DashboardDTO dashboardDTO = new DashboardDTO();
        dashboardDTO.setCrawledNewsList(todayNews);
        dashboardDTO.setCrawledIpoList(todayIpoList);
        dashboardDTO.setGoldMarketDailyTradingList(latestGoldMarket);
        dashboardDTO.setOilMarketDailyTradingList(latestOilMarket);
        dashboardDTO.setKospiDailyTradingList(latestKospiMarket);
        dashboardDTO.setKosdaqDailyTradingList(latestKosdaqMarket);
        dashboardDTO.setTopGainersList(topGainers);
        dashboardDTO.setTopVolumeList(topVolume);
        return ResponseEntity.ok(dashboardDTO);
    }
    
    @GetMapping("/test")
    public ResponseEntity test() {
	List<GoldMarketDailyTradingDTO> latestGoldMarket = krxDataService.getDailyGoldMarketTradingInfo();
	List<OilMarketDailyTradingDTO> latestOilMarket = krxDataService.getDailtyOilMarketTradingInfo();
	List<KospiDailyTradingDTO> latestKospiMarket = krxDataService.getDailyKospiInfo();
	List<KosdaqDailyTradingDTO> latestKosdaqMarket = krxDataService.getDailyKosdaqInfo();

	DashboardDTO dashboardDTO = new DashboardDTO();
	dashboardDTO.setGoldMarketDailyTradingList(latestGoldMarket);
	dashboardDTO.setOilMarketDailyTradingList(latestOilMarket);
	dashboardDTO.setKospiDailyTradingList(latestKospiMarket);
	dashboardDTO.setKosdaqDailyTradingList(latestKosdaqMarket);

	return ResponseEntity.ok(dashboardDTO);
    }

    @GetMapping("/chart-data")
    public ResponseEntity<ChartDataDTO> getChartData(
            @RequestParam(defaultValue = "KOSPI") String market,
            @RequestParam(defaultValue = "코스피") String indexName,
            @RequestParam(required = false) String isuCd,
            @RequestParam(defaultValue = "30") int limit) {

        ChartDataDTO chartData = new ChartDataDTO();
        chartData.setIndexName(indexName);

        if ("KOSPI".equalsIgnoreCase(market)) {
            List<KospiDailyTradingDTO> history = krxDataMapper.selectKospiHistory(indexName, limit);
            Collections.reverse(history);
            chartData.setLabels(history.stream().map(KospiDailyTradingDTO::getBasDd).collect(Collectors.toList()));
            chartData.setValues(history.stream().map(KospiDailyTradingDTO::getClsprcIdx).collect(Collectors.toList()));
        } else if ("KOSDAQ".equalsIgnoreCase(market)) {
            List<KosdaqDailyTradingDTO> history = krxDataMapper.selectKosdaqHistory(indexName, limit);
            Collections.reverse(history);
            chartData.setLabels(history.stream().map(KosdaqDailyTradingDTO::getBasDd).collect(Collectors.toList()));
            chartData.setValues(history.stream().map(KosdaqDailyTradingDTO::getClsprcIdx).collect(Collectors.toList()));
        } else if ("STOCK".equalsIgnoreCase(market) && isuCd != null) {
            List<StockDailyTradingDTO> history = krxDataMapper.selectStockHistory(isuCd, limit);
            Collections.reverse(history);
            chartData.setLabels(history.stream().map(StockDailyTradingDTO::getBasDd).collect(Collectors.toList()));
            chartData.setValues(history.stream().map(StockDailyTradingDTO::getTddClsprc).collect(Collectors.toList()));
            if (!history.isEmpty()) {
                chartData.setIndexName(history.get(0).getIsuNm());
            }
        }

        return ResponseEntity.ok(chartData);
    }

    @GetMapping("/stocks/search")
    public ResponseEntity<List<StockDailyTradingDTO>> searchStocks(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit) {
        List<StockDailyTradingDTO> stocks = krxDataMapper.searchStocks(keyword, limit);
        return ResponseEntity.ok(stocks);
    }

}
