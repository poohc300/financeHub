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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                if (href.isEmpty()) href = link.attr("href");
                href = normalizeNewsUrl(href);
                if (!title.isEmpty() && isValidArticleUrl(href)) {
                    NewsDTO dto = new NewsDTO();
                    dto.setTitle(title);
                    dto.setLink(href);
                    newsList.add(dto);
                    log.debug("뉴스 수집: [{}] {}", title, href);
                }
            }

            log.info("뉴스 크롤링 완료: {}건", newsList.size());
        } catch (Exception e) {
            log.error("뉴스 크롤링 오류: {}", e.getMessage(), e);
        }

        return newsList;
    }

    private boolean isValidArticleUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        return url.contains("article") || url.contains("news_read") || url.contains("articleid");
    }

    /**
     * finance.naver.com/news/news_read.naver?article_id=XXX&office_id=YYY
     * → https://n.news.naver.com/article/YYY/XXX  (모바일/데스크탑 모두 정상 작동)
     */
    private String normalizeNewsUrl(String url) {
        if (url == null) return url;
        Pattern p = Pattern.compile("[?&]article_id=(\\d+).*[?&]office_id=(\\d+)|[?&]office_id=(\\d+).*[?&]article_id=(\\d+)");
        Matcher m = p.matcher(url);
        if (m.find()) {
            String articleId = m.group(1) != null ? m.group(1) : m.group(4);
            String officeId  = m.group(2) != null ? m.group(2) : m.group(3);
            return "https://n.news.naver.com/article/" + officeId + "/" + articleId;
        }
        return url;
    }
}
