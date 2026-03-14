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


    @GetMapping("/chart-data")
    public ResponseEntity<ChartDataDTO> getChartData(
            @RequestParam(defaultValue = "KOSPI") String market,
            @RequestParam(defaultValue = "코스피") String indexName,
            @RequestParam(required = false) String isuCd,
            @RequestParam(defaultValue = "30") int limit,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        ChartDataDTO chartData = new ChartDataDTO();
        chartData.setIndexName(indexName);

        boolean useDateRange = startDate != null && endDate != null;

        if ("KOSPI".equalsIgnoreCase(market)) {
            List<KospiDailyTradingDTO> history = useDateRange
                    ? krxDataMapper.selectKospiHistoryByDateRange(indexName, startDate, endDate)
                    : krxDataMapper.selectKospiHistory(indexName, limit);
            Collections.reverse(history);
            chartData.setLabels(history.stream().map(KospiDailyTradingDTO::getBasDd).collect(Collectors.toList()));
            chartData.setValues(history.stream().map(KospiDailyTradingDTO::getClsprcIdx).collect(Collectors.toList()));
            chartData.setVolumes(history.stream().map(KospiDailyTradingDTO::getAccTrdvol).collect(Collectors.toList()));
            chartData.setOpens(history.stream().map(KospiDailyTradingDTO::getOpnprcIdx).collect(Collectors.toList()));
            chartData.setHighs(history.stream().map(KospiDailyTradingDTO::getHgprcIdx).collect(Collectors.toList()));
            chartData.setLows(history.stream().map(KospiDailyTradingDTO::getLwprcIdx).collect(Collectors.toList()));
        } else if ("KOSDAQ".equalsIgnoreCase(market)) {
            List<KosdaqDailyTradingDTO> history = useDateRange
                    ? krxDataMapper.selectKosdaqHistoryByDateRange(indexName, startDate, endDate)
                    : krxDataMapper.selectKosdaqHistory(indexName, limit);
            Collections.reverse(history);
            chartData.setLabels(history.stream().map(KosdaqDailyTradingDTO::getBasDd).collect(Collectors.toList()));
            chartData.setValues(history.stream().map(KosdaqDailyTradingDTO::getClsprcIdx).collect(Collectors.toList()));
            chartData.setVolumes(history.stream().map(KosdaqDailyTradingDTO::getAccTrdvol).collect(Collectors.toList()));
            chartData.setOpens(history.stream().map(KosdaqDailyTradingDTO::getOpnprcIdx).collect(Collectors.toList()));
            chartData.setHighs(history.stream().map(KosdaqDailyTradingDTO::getHgprcIdx).collect(Collectors.toList()));
            chartData.setLows(history.stream().map(KosdaqDailyTradingDTO::getLwprcIdx).collect(Collectors.toList()));
        } else if ("STOCK".equalsIgnoreCase(market) && isuCd != null) {
            List<StockDailyTradingDTO> history = useDateRange
                    ? krxDataMapper.selectStockHistoryByDateRange(isuCd, startDate, endDate)
                    : krxDataMapper.selectStockHistory(isuCd, limit);
            Collections.reverse(history);
            chartData.setLabels(history.stream().map(StockDailyTradingDTO::getBasDd).collect(Collectors.toList()));
            chartData.setValues(history.stream().map(StockDailyTradingDTO::getTddClsprc).collect(Collectors.toList()));
            chartData.setVolumes(history.stream().map(StockDailyTradingDTO::getAccTrdvol).collect(Collectors.toList()));
            chartData.setOpens(history.stream().map(StockDailyTradingDTO::getTddOpnprc).collect(Collectors.toList()));
            chartData.setHighs(history.stream().map(StockDailyTradingDTO::getTddHgprc).collect(Collectors.toList()));
            chartData.setLows(history.stream().map(StockDailyTradingDTO::getTddLwprc).collect(Collectors.toList()));
            if (!history.isEmpty()) {
                chartData.setIndexName(history.get(0).getIsuNm());
            }
        } else if ("GOLD".equalsIgnoreCase(market)) {
            List<GoldMarketDailyTradingDTO> history = useDateRange
                    ? krxDataMapper.selectGoldHistoryByDateRange(indexName, startDate, endDate)
                    : krxDataMapper.selectGoldHistory(indexName, limit);
            Collections.reverse(history);
            chartData.setLabels(history.stream().map(GoldMarketDailyTradingDTO::getBasDd).collect(Collectors.toList()));
            chartData.setValues(history.stream().map(GoldMarketDailyTradingDTO::getTddClsprc).collect(Collectors.toList()));
            chartData.setVolumes(history.stream().map(GoldMarketDailyTradingDTO::getAccTrdvol).collect(Collectors.toList()));
            chartData.setOpens(history.stream().map(GoldMarketDailyTradingDTO::getTddOpnprc).collect(Collectors.toList()));
            chartData.setHighs(history.stream().map(GoldMarketDailyTradingDTO::getTddHgprc).collect(Collectors.toList()));
            chartData.setLows(history.stream().map(GoldMarketDailyTradingDTO::getTddLwprc).collect(Collectors.toList()));
        } else if ("OIL".equalsIgnoreCase(market)) {
            List<OilMarketDailyTradingDTO> history = useDateRange
                    ? krxDataMapper.selectOilHistoryByDateRange(indexName, startDate, endDate)
                    : krxDataMapper.selectOilHistory(indexName, limit);
            Collections.reverse(history);
            chartData.setLabels(history.stream().map(OilMarketDailyTradingDTO::getBasDd).collect(Collectors.toList()));
            chartData.setValues(history.stream().map(OilMarketDailyTradingDTO::getWtAvgPrc).collect(Collectors.toList()));
            chartData.setVolumes(history.stream().map(OilMarketDailyTradingDTO::getAccTrdvol).collect(Collectors.toList()));
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
