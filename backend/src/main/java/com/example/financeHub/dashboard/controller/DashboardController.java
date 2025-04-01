package com.example.financeHub.dashboard.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.financeHub.crawler.model.CrawledIpoDTO;
import com.example.financeHub.crawler.model.CrawledNewsDTO;
import com.example.financeHub.crawler.service.IpoCrawlerService;
import com.example.financeHub.crawler.service.NewsCrawlerService;
import com.example.financeHub.crawler.service.NewsCrawlingService;
import com.example.financeHub.dashboard.model.DashboardDTO;
import com.example.financeHub.dashboard.model.GoldMarketDailyTradingDto;
import com.example.financeHub.dashboard.service.KrxDataService;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {
    
   
    private final NewsCrawlerService newsCrawlerService;
    private final IpoCrawlerService ipoCrawlerService;
    private final KrxDataService krxDataService;
    
    public DashboardController(
	    NewsCrawlerService newsCrawlerService,
	    IpoCrawlerService ipoCrawlerService,
	    KrxDataService krxDataService
	    ) {
	this.newsCrawlerService = newsCrawlerService;
	this.ipoCrawlerService = ipoCrawlerService;
	this.krxDataService = krxDataService;
    }
    
    
    
    @GetMapping("/data")
    public ResponseEntity getData() {
	List<CrawledNewsDTO> todayNews = newsCrawlerService.getTodayNews();
	List<CrawledIpoDTO> todayIpoList = ipoCrawlerService.getTodayIpoList();
	List<GoldMarketDailyTradingDto> todayGoldMarket = krxDataService.getDailyGoldMarketTradingInfo();
	
	
	DashboardDTO dashboardDTO = new DashboardDTO();
	dashboardDTO.setCrawledNewsList(todayNews);
	dashboardDTO.setCrawledIpoList(todayIpoList);
	dashboardDTO.setGoldMarketDailyTradingList(todayGoldMarket);
	return ResponseEntity.ok(dashboardDTO);
    }
    
    @GetMapping("/test")
    public ResponseEntity test() {
	List<GoldMarketDailyTradingDto> todayGoldMarket = krxDataService.getDailyGoldMarketTradingInfo();
	DashboardDTO dashboardDTO = new DashboardDTO();
	dashboardDTO.setGoldMarketDailyTradingList(todayGoldMarket);
	return ResponseEntity.ok(dashboardDTO);


    }

}
