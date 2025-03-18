package com.example.financeHub.crawler.service;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

import com.example.financeHub.crawler.model.CrawledIpoDTO;
import com.example.financeHub.crawler.model.CrawledNewsDTO;

@Service
public class IpoCrawlerService {

    private static final String BASE_URL = "https://www.38.co.kr/html/fund/?o=k";

    private final ChromeOptions options;

    public IpoCrawlerService(ChromeOptions options) {
	this.options = options;
    }

    public List<CrawledIpoDTO> crawler() {
	WebDriver driver = null; // WebDriver를 메서드 내에서 선언
	List<CrawledIpoDTO> ipoList = new ArrayList<>();

	try {
	    driver = new ChromeDriver(options); // WebDriver 인스턴스 생성
	    driver.get(BASE_URL); // 네이버 경제 뉴스 페이지 이동

	    // 공모주 청약 일정 테이블 
	    WebElement ulElement = driver
		    .findElement(By.cssSelector("body > table:nth-child(9) > tbody > tr > td > table:nth-child(2) > tbody > tr > td:nth-child(1) > table:nth-child(11) > tbody > tr:nth-child(2) > td > table > tbody"));
	    List<WebElement> rows = ulElement.findElements(By.tagName("tr"));

	    // tr 별 추출 
	    for (WebElement row : rows) {
		List<WebElement> cells = row.findElements(By.tagName("td"));
		// DTO로 저장
		CrawledIpoDTO ipoDTO = new CrawledIpoDTO();
		
		WebElement linkElement = cells.get(0).findElement(By.tagName("a"));
		String companyName = linkElement.getText().trim();
		String link = linkElement.getAttribute("href");
		
		String period = cells.get(1).getText().trim();
		String fixedOfferingPrice = cells.get(2).getText().trim();
		String expectedOfferingPrice = cells.get(3).getText().trim();
		String subscriptionRate = cells.get(4).getText().trim();
		String underWriter = cells.get(5).getText().trim();
		
		ipoDTO.setCompanyName(companyName);
		ipoDTO.setLink(link);
		ipoDTO.setPeriod(period);
		ipoDTO.setFixedOfferingPrice(fixedOfferingPrice);
		ipoDTO.setExpectedOfferingPrice(expectedOfferingPrice);
		ipoDTO.setSubscriptionRate(subscriptionRate);
		ipoDTO.setUnderWriter(underWriter);
		
		ipoList.add(ipoDTO);
	    }
	} catch (Exception e) {
	    e.printStackTrace(); // 오류 로그 출력
	} finally {
	    if (driver != null) {
		driver.quit(); // WebDriver 리소스 해제
	    }
	}

	return ipoList;
    }
    
    public List<CrawledIpoDTO> getTodayIpoList() {
	List<CrawledIpoDTO> ipoList = crawler();
	return ipoList;
    }
}
