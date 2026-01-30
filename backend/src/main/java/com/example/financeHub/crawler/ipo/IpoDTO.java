package com.example.financeHub.crawler.ipo;
import com.example.financeHub.config.HashUtil;

import lombok.Data;

@Data
public class IpoDTO {

    private String companyName; // 종목명
    private String link; // 링크
    private String period; // 공모주일정 
    private String fixedOfferingPrice; // 확정공모가 
    private String expectedOfferingPrice; // 희암공모가
    private String subscriptionRate; // 청약경쟁률
    private String underWriter; // 주간사
    private String dataHash; // 데이터 해시

    public String generateHash() {
	String key = companyName + "|" + link + "|" + period;
	return HashUtil.generateHash(key);
    }
}
