package com.example.financeHub.dashboard.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.naming.java.javaURLContextFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.financeHub.dashboard.model.GoldMarketDailyTradingDto;

@Service
public class KrxDataService {
    
    @Value("${KRX_API_KEY}")
    private String apiKey;
    
    private final String goldUrl = "http://data-dbg.krx.co.kr/svc/apis/gen/gold_bydd_trd";
    private final RestTemplate restTemplate;
    
    public KrxDataService(RestTemplate restTemplate) {
	this.restTemplate = restTemplate;
    }
    
    public List<GoldMarketDailyTradingDto> getDailyGoldMarketTradingInfo() {
	
	List<GoldMarketDailyTradingDto> result = new ArrayList<>();
	
	try {
	    // 오늘 날짜 가져오기 
	    LocalDate today = LocalDate.now();
	    String formattedDate = today.format(DateTimeFormatter.BASIC_ISO_DATE); //YYYYMMDD
	    

            // 요청 URL 생성
            String url = "http://data-dbg.krx.co.kr/svc/apis/gen/gold_bydd_trd?basDd=" + formattedDate;

            // 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("AUTH_KEY", apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // API 호출 (Blocking 방식)
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );
         // 응답 데이터 처리
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                System.out.println(response);
                if (responseBody.containsKey("OutBlock_1")) {
                    List<Map<String, Object>> outBlockData = (List<Map<String, Object>>) responseBody.get("OutBlock_1");
                    
                    for (Map<String, Object> item : outBlockData) {
                        GoldMarketDailyTradingDto dto = new GoldMarketDailyTradingDto();
                        dto.setBasDd(getString(item, "BAS_DD"));
                        dto.setIsuCd(getString(item, "ISU_CD"));
                        dto.setIsuNm(getString(item, "ISU_NM"));
                        dto.setTddClsprc(getString(item, "TDD_CLSPRC"));
                        dto.setCmpprevddPrc(getString(item, "CMPPREVDD_PRC"));
                        dto.setFlucRt(getString(item, "FLUC_RT"));
                        dto.setTddOpnprc(getString(item, "TDD_OPNPRC"));
                        dto.setTddHgprc(getString(item, "TDD_HGPRC"));
                        dto.setTddLwprc(getString(item, "TDD_LWPRC"));
                        dto.setAccTrdvol(getString(item, "ACC_TRDVOL"));
                        dto.setAccTrdval(getString(item, "ACC_TRDVAL"));
                        result.add(dto);
                    }
                }
            }
		
		
	} catch (Exception e) {
            System.err.println("API 호출 중 오류 발생: " + e.getMessage());
	}
	return result;
    }
    
    // 안전한 문자열 변환 메서드
    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

}
