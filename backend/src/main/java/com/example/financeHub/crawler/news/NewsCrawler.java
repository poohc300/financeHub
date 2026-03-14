package com.example.financeHub.crawler.news;

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
public class NewsCrawler {

    private static final Logger log = LoggerFactory.getLogger(NewsCrawler.class);
    private static final String BASE_URL = "https://finance.naver.com/news/";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36";

    public List<NewsDTO> getTodayNews() {
        List<NewsDTO> newsList = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(BASE_URL)
                    .userAgent(USER_AGENT)
                    .timeout(15000)
                    .get();

            // 메인 뉴스 ul > li > a
            Elements links = doc.select("#newsMainTop .main_news ul li a");

            for (Element link : links) {
                String title = link.text().trim();
                String href = link.absUrl("href");
                if (!title.isEmpty() && !href.isEmpty()) {
                    NewsDTO dto = new NewsDTO();
                    dto.setTitle(title);
                    dto.setLink(href);
                    newsList.add(dto);
                }
            }

            log.info("뉴스 크롤링 완료: {}건", newsList.size());
        } catch (Exception e) {
            log.error("뉴스 크롤링 오류: {}", e.getMessage(), e);
        }

        return newsList;
    }
}
