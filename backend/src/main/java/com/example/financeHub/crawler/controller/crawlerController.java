package com.example.financeHub.crawler.controller;

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
import com.example.financeHub.crawler.service.IpoCrawlingService;
import com.example.financeHub.crawler.service.NewsCrawlerService;
import com.example.financeHub.crawler.service.NewsCrawlingService;

@RestController
@RequestMapping("/crawler")
public class crawlerController {

    
    private final NewsCrawlerService newsCrawlerService;
    private final IpoCrawlerService ipoCrawlerService;
    
    public crawlerController(NewsCrawlerService newsCrawlerService, IpoCrawlerService ipoCrawlerService) {
	this.newsCrawlerService = newsCrawlerService;
	this.ipoCrawlerService = ipoCrawlerService;
    }
    
    @GetMapping("/getTodayNews")
    public ResponseEntity getTodayNews() {
	List<CrawledNewsDTO> result = newsCrawlerService.getTodayNews();
	return ResponseEntity.ok(result);
    }
    
    @GetMapping("/getTodayIpoList")
    public ResponseEntity getTodayIpoList() {
   	List<CrawledIpoDTO> result = ipoCrawlerService.getTodayIpoList();
   	return ResponseEntity.ok(result);
       }
    
}
