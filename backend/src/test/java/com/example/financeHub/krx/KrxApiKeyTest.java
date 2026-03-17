package com.example.financeHub.krx;

import com.example.financeHub.krx.util.KrxApiCaller;
import com.example.financeHub.krx.util.KrxCommonUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class KrxApiKeyTest {

    private static final Logger log = LoggerFactory.getLogger(KrxApiKeyTest.class);

    @Autowired
    private KrxApiCaller krxApiCaller;

    @Value("${KRX_API_KEY}")
    private String apiKey;

    /** API 키가 비어있지 않은지 확인 (환경변수 주입 여부) */
    @Test
    void apiKey_shouldBeInjected() {
        log.info("주입된 KRX_API_KEY: [{}]", apiKey);
        assertThat(apiKey)
                .as("KRX_API_KEY 환경변수가 비어있습니다. .env 파일을 확인하세요.")
                .isNotBlank();
    }

    /** KOSPI 엔드포인트로 실제 API 호출해서 응답 데이터가 오는지 확인 */
    @Test
    void kospiApi_shouldReturnData() {
        LocalDate date = KrxCommonUtil.getLastTradingDay();
        String formattedDate = KrxCommonUtil.formatDate(date);

        log.info("요청 날짜: {}", formattedDate);

        List<Map<String, Object>> result = krxApiCaller.callApi("idx/kospi_dd_trd", formattedDate);

        log.info("응답 건수: {}", result.size());
        if (!result.isEmpty()) {
            log.info("첫 번째 데이터: {}", result.get(0));
        }

        assertThat(result)
                .as("KOSPI API 응답이 비어있습니다. API 키 또는 날짜(%s)를 확인하세요.", formattedDate)
                .isNotEmpty();
    }
}
