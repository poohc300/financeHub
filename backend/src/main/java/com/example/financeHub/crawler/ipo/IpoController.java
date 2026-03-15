package com.example.financeHub.crawler.ipo;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.financeHub.crawler.mapper.CrawlerDataMapper;

@RestController
@RequestMapping("/ipo")
public class IpoController {

    private final CrawlerDataMapper crawlerDataMapper;

    public IpoController(CrawlerDataMapper crawlerDataMapper) {
        this.crawlerDataMapper = crawlerDataMapper;
    }

    @GetMapping("/list")
    public ResponseEntity<List<IpoDTO>> getIpoList(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        List<IpoDTO> list = crawlerDataMapper.selectFilteredIpo(startDate, endDate, keyword, Math.min(limit, 100), offset);
        return ResponseEntity.ok(list);
    }
}
