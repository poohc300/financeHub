package com.example.financeHub.scheduler;

import org.springframework.stereotype.Component;

import com.example.financeHub.crawler.ipo.IpoCrawler;
import com.example.financeHub.crawler.news.NewsCrawler;
import com.example.financeHub.krx.service.KrxDataService;

@Component
public class DataFetchScheduler {

    private final IpoCrawler ipoCrawlerService;
    private final NewsCrawler newsCrawlerService;
    private final KrxDataService krxDataService;
    
    public DataFetchScheduler(
	    IpoCrawler ipoCrawlerService, 
	    NewsCrawler newsCrawlerService, 
	    KrxDataService krxDataService) {
	this.ipoCrawlerService = ipoCrawlerService;
	this.newsCrawlerService = newsCrawlerService;
	this.krxDataService = krxDataService;
    }
    
    
}
