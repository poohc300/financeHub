package com.example.financeHub.crawler.ipo;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class IpoCrawler {

    private static final Logger log = LoggerFactory.getLogger(IpoCrawler.class);
    private static final String BASE_URL = "https://www.38.co.kr/html/fund/?o=k";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36";

    public List<IpoDTO> getTodayIpoList() {
        List<IpoDTO> ipoList = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(BASE_URL)
                    .userAgent(USER_AGENT)
                    .timeout(15000)
                    .get();

            // 공모주 청약 일정 테이블 행 파싱
            // 38.co.kr은 table 기반 구조 - 헤더 포함 tr에서 셀이 6개 이상인 행만 처리
            Elements rows = doc.select("table tbody tr");

            for (Element row : rows) {
                Elements cells = row.select("td");
                if (cells.size() < 6) continue;

                Element linkEl = cells.get(0).selectFirst("a");
                if (linkEl == null) continue;

                String companyName = linkEl.text().trim();
                String link = linkEl.absUrl("href");
                String period = cells.get(1).text().trim();
                String fixedOfferingPrice = cells.get(2).text().trim();
                String expectedOfferingPrice = cells.get(3).text().trim();
                String subscriptionRate = cells.get(4).text().trim();
                String underWriter = cells.get(5).text().trim();

                if (companyName.isEmpty()) continue;

                IpoDTO dto = new IpoDTO();
                dto.setCompanyName(companyName);
                dto.setLink(link);
                dto.setPeriod(period);
                dto.setFixedOfferingPrice(fixedOfferingPrice);
                dto.setExpectedOfferingPrice(expectedOfferingPrice);
                dto.setSubscriptionRate(subscriptionRate);
                dto.setUnderWriter(underWriter);
                ipoList.add(dto);
            }

            log.info("공모주 크롤링 완료: {}건", ipoList.size());
        } catch (Exception e) {
            log.error("공모주 크롤링 오류: {}", e.getMessage(), e);
        }

        return ipoList;
    }
}
