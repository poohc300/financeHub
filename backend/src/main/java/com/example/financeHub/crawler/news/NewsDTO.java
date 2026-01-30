package com.example.financeHub.crawler.news;
import com.example.financeHub.config.HashUtil;

import lombok.Data;

@Data
public class NewsDTO {

    private String title;        // 뉴스 제목
    private String link;         // 뉴스 링크
    private String publishedAt;  // 발행일
    private String dataHash;     // 데이터 해시

    public String generateHash() {
	String key = title + "|" + link;
	return HashUtil.generateHash(key);
    }
}
