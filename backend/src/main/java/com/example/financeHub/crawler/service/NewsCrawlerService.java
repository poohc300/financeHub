package com.example.financeHub.crawler.service;

import com.example.financeHub.crawler.model.CrawledNewsDTO;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NewsCrawlerService {

    private static final String BASE_URL = "https://finance.naver.com/news/";

    private final ChromeOptions options;

    public NewsCrawlerService(ChromeOptions options) {
        this.options = options;
    }

    public List<CrawledNewsDTO> crawler() {
        WebDriver driver = null; // WebDriver를 메서드 내에서 선언
        List<CrawledNewsDTO> newsList = new ArrayList<>();

        try {
            driver = new ChromeDriver(options); // WebDriver 인스턴스 생성
            driver.get(BASE_URL); // 네이버 경제 뉴스 페이지 이동

            // 뉴스 리스트 찾기
            WebElement ulElement = driver.findElement(By.cssSelector("#newsMainTop > div > div > div > div.main_news > ul"));
            List<WebElement> listItems = ulElement.findElements(By.tagName("li"));

            // 뉴스 제목과 링크 추출
            for (WebElement element : listItems) {
                WebElement linkElement = element.findElement(By.tagName("a")); // a 태그 가져오기
                String title = linkElement.getText(); // 뉴스 제목
                String link = linkElement.getAttribute("href"); // 뉴스 링크

                // DTO로 저장
                CrawledNewsDTO newsDTO = new CrawledNewsDTO();
                newsDTO.setTitle(title);
                newsDTO.setLink(link);
                newsList.add(newsDTO);
            }
        } catch (Exception e) {
            e.printStackTrace(); // 오류 로그 출력
        } finally {
            if (driver != null) {
                driver.quit(); // WebDriver 리소스 해제
            }
        }

        return newsList;
    }

    // 오늘의 뉴스를 가져오는 메서드
    public List<CrawledNewsDTO> getTodayNews() {
        return crawler(); // 크롤링 메서드 호출
    }
    
    
}
