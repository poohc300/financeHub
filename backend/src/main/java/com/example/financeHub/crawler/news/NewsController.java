package com.example.financeHub.crawler.news;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.financeHub.crawler.mapper.CrawlerDataMapper;

@RestController
@RequestMapping("/news")
public class NewsController {

    private final CrawlerDataMapper crawlerDataMapper;

    public NewsController(CrawlerDataMapper crawlerDataMapper) {
        this.crawlerDataMapper = crawlerDataMapper;
    }

    @GetMapping("/list")
    public ResponseEntity<List<NewsDTO>> getNewsList(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        List<NewsDTO> news = crawlerDataMapper.selectFilteredNews(startDate, endDate, keyword, limit, offset);
        return ResponseEntity.ok(news);
    }
}
