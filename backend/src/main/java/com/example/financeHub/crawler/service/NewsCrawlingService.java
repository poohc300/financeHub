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
    }

    // 크롤링 메서드
    public List<CrawledNewsDTO> crawler() {
        List<CrawledNewsDTO> newsList = new ArrayList<>();

        try {
            driver = new ChromeDriver(options);
            // 네이버 경제 뉴스 페이지 이동
            driver.get(BASE_URL);

            // 뉴스 항목 찾기 (CSS Selector 사용)
            
            // ul 태그 가져오기
            WebElement ulElement = driver.findElement(By.cssSelector("#newsMainTop > div > div > div > div.main_news > ul"));

            // ul 내의 li 항목 가져오기
            List<WebElement> listItems = ulElement.findElements(By.tagName("li"));

            // 각 뉴스 제목과 링크 추출 및 DTO로 저장
            for (WebElement element : listItems) {
        	// a 태그를 가져와야 href 가져올 수 있음
        	WebElement linkElement = element.findElement(By.tagName("a"));
                String title = linkElement.getText();
                String link = linkElement.getAttribute("href");

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
        return newsList;
    }
    
    public List<CrawledNewsDTO> getTodayNews() {
	List<CrawledNewsDTO> result = new ArrayList<>();
	result = crawler();
	
	return result; 
    }
}
