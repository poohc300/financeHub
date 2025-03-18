package com.example.financeHub.crawler.model;
import lombok.Data;

@Data
public class CrawledIpoDTO {

    private String companyName; // 종목명
    private String link; // 링크
    private String period; // 공모주일정 
    private String fixedOfferingPrice; // 확정공모가 
    private String expectedOfferingPrice; // 희암공모가
    private String subscriptionRate; // 청약경쟁률
    private String underWriter; // 주간사 
}
