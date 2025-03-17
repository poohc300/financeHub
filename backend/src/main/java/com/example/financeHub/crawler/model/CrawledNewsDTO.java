package com.example.financeHub.crawler.model;
import lombok.Data;

@Data
public class CrawledNewsDTO {

    private String title;        // 뉴스 제목
    private String link;         // 뉴스 링크
}
