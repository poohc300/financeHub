package com.example.financeHub.config;

import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class WebDriverConfig {

    @Bean
    public ChromeOptions chromeOptions() {
        ChromeOptions options = new ChromeOptions();

        // OS/아키텍처 자동 감지
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        // Linux ARM (라즈베리파이) - snap chromium 사용
        if (os.contains("linux") && (arch.contains("aarch64") || arch.contains("arm"))) {
            String chromiumPath = "/snap/bin/chromium";
            String chromedriverPath = "/snap/bin/chromium.chromedriver";

            if (new File(chromiumPath).exists()) {
                options.setBinary(chromiumPath);
                System.setProperty("webdriver.chrome.driver", chromedriverPath);
            }

            // ARM 필수 옵션
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
        }

        // 공통 옵션
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--disable-default-apps");
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--headless");

        return options;
    }
}
