package com.example.financeHub.krx.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class KrxApiCaller {
    
    @Value("${KRX_API_KEY}")
    private String apiKey; // api키는 static이 아니고 인스턴스를 통해 받아야 함
    private static final String baseUrl = "http://data-dbg.krx.co.kr/svc/apis/";
    private final RestTemplate restTemplate;
    
    // 생성자에서 RestTemplate 주입
    public KrxApiCaller(RestTemplate restTemplate) {
	this.restTemplate = restTemplate;
    }
    
    /**
     * 요청헤더 설정 및 API 호출 
     * 
     */
    public List<Map<String, Object>> callApi(String apiUrl, String formattedDate) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        
        try {
 	   // 요청 URL 설정
 	       String url = baseUrl + apiUrl + "?basDd=" + formattedDate;
 	       
 	       // 요청 헤더 설정 
 	       HttpHeaders headers = new HttpHeaders();
 	       headers.set("AUTH_KEY", apiKey);
 	       
 	       // 요청 엔티티 설정 
 	       HttpEntity<String> entity = new HttpEntity<>(headers);
 	       
 	       // API 호출 
 	       ResponseEntity<Map> response = restTemplate.exchange(
 		       url,
 		       HttpMethod.GET,
 		       entity,
 		       Map.class
 		       );
 	       
 	       // 응답 데이터 처리 
 	       if(response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
 		   Map<String, Object> responseBody = response.getBody();
 		   
 		   if(responseBody.containsKey("OutBlock_1")) {
 	               List<Map<String, Object>> outBlockData = (List<Map<String, Object>>) responseBody.get("OutBlock_1");
 	               resultList.addAll(outBlockData);
 		   }
 	       } 
         } catch (Exception e) {
             System.err.println("API 호출 중 예외 발생: " + e.getMessage());
         }
       
        return resultList;   
    }
}
