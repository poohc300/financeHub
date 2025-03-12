package com.example.financeHub.crawler.model;
import lombok.Data;

@Data
public class CrawledNewsDTO {

    private String title;        // 뉴스 제목
    private String link;         // 뉴스 링크
    private String description;  // 뉴스 설명 (옵션)
    private String publicationDate; // 뉴스 작성 시간 (옵션)
}
