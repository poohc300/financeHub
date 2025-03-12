package com.example.financeHub.crawler.service;

import com.example.financeHub.crawler.model.CrawledNewsDTO;

import ch.qos.logback.core.net.SyslogOutputStream;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

@Service
public class NewsCrawlingService {

    private WebDriver driver;
    private ChromeOptions options;
    private static final String BASE_URL = "https://finance.naver.com/news/";

    // 생성자에서 WebDriver 초기화
    public NewsCrawlingService() {
        // ChromeOptions 설정
        options = new ChromeOptions();
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--disable-default-apps");
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-blink-features=AutomationControlled");

        // Selenium Manager를 활용한 WebDriver 초기화 (Selenium 4.6 이상에서 동작)
        driver = new ChromeDriver(options);
    }

    // 크롤링 메서드
    public List<CrawledNewsDTO> crawler() {
        List<CrawledNewsDTO> newsList = new ArrayList<>();

        try {
            // 네이버 경제 뉴스 페이지 이동
            driver.get(BASE_URL);

            // 뉴스 항목 찾기 (CSS Selector 사용)
            List<WebElement> newsElements = driver.findElements(By.cssSelector(".newsList li a"));

            // 각 뉴스 제목과 링크 추출 및 DTO로 저장
            for (WebElement element : newsElements) {
                String title = element.getText();
                String link = element.getAttribute("href");

                // DTO 객체 생성 및 리스트 추가
                CrawledNewsDTO newsDTO = new CrawledNewsDTO();
                newsDTO.setTitle(title);
                newsDTO.setLink(link);
                newsList.add(newsDTO);
            }
        } catch (Exception e) {
            e.printStackTrace(); // 오류 발생 시 로그 출력
        } finally {
            // WebDriver 종료
            if (driver != null) {
                driver.quit();
            }
        }
        System.out.println(newsList);
        return newsList;
    }
}
