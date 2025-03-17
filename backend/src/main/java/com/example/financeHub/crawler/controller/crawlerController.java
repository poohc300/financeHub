package com.example.financeHub.crawler.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.financeHub.crawler.model.CrawledNewsDTO;
import com.example.financeHub.crawler.service.NewsCrawlingService;

@RestController
@RequestMapping("/crawler")
public class crawlerController {

    @Autowired
    @Lazy
    private NewsCrawlingService crawlerService;
    
    @GetMapping("/getTodayNews")
    public ResponseEntity getTodayNews() {
	List<CrawledNewsDTO> result = crawlerService.getTodayNews();
	return ResponseEntity.ok(result);
    }
    
}
