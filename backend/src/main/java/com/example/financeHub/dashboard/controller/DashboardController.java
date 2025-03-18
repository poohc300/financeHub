package com.example.financeHub.dashboard.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.financeHub.crawler.model.CrawledNewsDTO;
import com.example.financeHub.crawler.service.IpoCrawlerService;
import com.example.financeHub.crawler.service.NewsCrawlerService;
import com.example.financeHub.crawler.service.NewsCrawlingService;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {
    
   
    private final NewsCrawlerService newsCrawlerService;
    private final IpoCrawlerService ipoCrawlerService;
    
    public DashboardController(NewsCrawlerService newsCrawlerService, IpoCrawlerService ipoCrawlerService) {
	this.newsCrawlerService = newsCrawlerService;
	this.ipoCrawlerService = ipoCrawlerService;
    }
    
    
    
    @GetMapping("/data")
    public ResponseEntity getData() {
	List<CrawledNewsDTO> result = newsCrawlerService.getTodayNews();
	return ResponseEntity.ok(result);
    }

}
