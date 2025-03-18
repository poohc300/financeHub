package com.example.financeHub.config;

import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebDriverConfig {

    @Bean
    public ChromeOptions chromeOptions() {
        ChromeOptions options = new ChromeOptions();
	options.addArguments("--disable-popup-blocking");
	options.addArguments("--disable-default-apps");
	options.addArguments("--disable-notifications");
	options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--headless"); // 브라우저를 숨김 모드로 실행
	return options;
    }
}
