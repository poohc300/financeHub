package com.example.financeHub.dashboard.service;

import java.lang.invoke.StringConcatFactory;
import java.time.DayOfWeek;
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

import com.example.financeHub.dashboard.model.GoldMarketDailyTradingDTO;
import com.example.financeHub.dashboard.model.KosdaqDailyTradingDTO;
import com.example.financeHub.dashboard.model.KospiDailyTradingDTO;
import com.example.financeHub.dashboard.model.OilMarketDailyTradingDTO;

import net.bytebuddy.matcher.MethodReturnTypeMatcher;

@Service
public class KrxDataService {
    
    @Value("${KRX_API_KEY}")
    private String apiKey;
    private final String baseUrl = "http://data-dbg.krx.co.kr/svc/apis/";
    private final String goldUrl = "gen/gold_bydd_trd";
    private final String oilUrl = "gen/oil_bydd_trd";
    private final String kospiUrl = "idx/kospi_dd_trd";
    private final String kosdaqUrl = "idx/kosdaq_dd_trd";
    private final RestTemplate restTemplate;
    
    public KrxDataService(RestTemplate restTemplate) {
	this.restTemplate = restTemplate;
    }
    
    public List<GoldMarketDailyTradingDTO> getDailyGoldMarketTradingInfo() {
	
	List<GoldMarketDailyTradingDTO> result = new ArrayList<>();
	
	try {
	    // 트레이딩 날짜 가져오기 
	    LocalDate date = getLastTradingDay();
	    String formattedDate = date.format(DateTimeFormatter.BASIC_ISO_DATE); //YYYYMMDD
	    

            // 요청 URL 생성
            String url = baseUrl + goldUrl + "?basDd=" + formattedDate;

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
                        GoldMarketDailyTradingDTO dto = new GoldMarketDailyTradingDTO();
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
    
    public List<OilMarketDailyTradingDTO> getDailtyOilMarketTradingInfo() {
	List<OilMarketDailyTradingDTO> result = new ArrayList<>();
	
	try {
	    // 트레이딩 날짜 가져오기 
	    LocalDate date = getLastTradingDay();
	    String formattedDate = date.format(DateTimeFormatter.BASIC_ISO_DATE); //YYYYMMDD
	    

            // 요청 URL 생성
            String url = baseUrl + oilUrl + "?basDd=" + formattedDate;

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
                        OilMarketDailyTradingDTO dto = new OilMarketDailyTradingDTO();
                        dto.setBasDd(getString(item, "BAS_DD"));
                        dto.setOilNm(getString(item, "OIL_NM"));
                        dto.setWtAvgPrc(getString(item, "WT_AVG_PRC"));
                        dto.setWtDisAvgPrc(getString(item, "WT_DIS_AVG_PRC"));
                        dto.setAccTrdvol(getString(item, "ACC_TRDVOL"));
                        dto.setAccTrdVal(getString(item, "ACC_TRDVAL"));
                        result.add(dto);
                    }
                }
            }
		
	} catch (Exception e) {
            System.err.println("API 호출 중 오류 발생: " + e.getMessage());

	}
	
	return result;
    }
    
    public List<KospiDailyTradingDTO> getDailyKospiInfo() {
	List<KospiDailyTradingDTO> result = new ArrayList<>();
	
	try {
	    // 트레이딩 날짜 가져오기 
	    LocalDate date = getLastTradingDay();
	    String formattedDate = date.format(DateTimeFormatter.BASIC_ISO_DATE); //YYYYMMDD
	    

            // 요청 URL 생성
            String url = baseUrl + kospiUrl + "?basDd=" + formattedDate;

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
                        KospiDailyTradingDTO dto = new KospiDailyTradingDTO();
                        dto.setBasDd(getString(item, "BAS_DD"));
                        dto.setIdxClss(getString(item, "IDX_CLSS"));
                        dto.setIdxNm(getString(item, "IDX_NM"));
                        dto.setClsprcIdx(getString(item, "CLSPRC_IDX"));
                        dto.setCmpprevddIdx(getString(item, "CMPPREVDD_IDX"));
                        dto.setFlucRt(getString(item, "FLUC_RT"));
                        dto.setOpnprcIdx(getString(item, "OPNPRC_IDX"));
                        dto.setHgprcIdx(getString(item, "HGPRC_IDX"));
                        dto.setLwprcIdx(getString(item, "LWPRC_IDX"));
                        dto.setAccTrdvol(getString(item, "ACC_TRDVOL"));
                        dto.setAccTrdval(getString(item, "ACC_TRDVAL"));
                        dto.setMktcap(getString(item, "MKTCAP"));
                        result.add(dto);
                    }
                }
            }
	} catch (Exception e) {
            System.err.println("API 호출 중 오류 발생: " + e.getMessage());
	}
	return result;
    }
    
    public List<KosdaqDailyTradingDTO> getDailyKosdaqInfo() {
	List<KosdaqDailyTradingDTO> result = new ArrayList<>();

	
	try {
	    // 트레이딩 날짜 가져오기 
	    LocalDate date = getLastTradingDay();
	    String formattedDate = date.format(DateTimeFormatter.BASIC_ISO_DATE); //YYYYMMDD
	    

            // 요청 URL 생성
            String url = baseUrl + kosdaqUrl + "?basDd=" + formattedDate;

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
                        KosdaqDailyTradingDTO dto = new KosdaqDailyTradingDTO();
                        dto.setBasDd(getString(item, "BAS_DD"));
                        dto.setIdxClss(getString(item, "IDX_CLSS"));
                        dto.setIdxNm(getString(item, "IDX_NM"));
                        dto.setClsprcIdx(getString(item, "CLSPRC_IDX"));
                        dto.setCmpprevddIdx(getString(item, "CMPPREVDD_IDX"));
                        dto.setFlucRt(getString(item, "FLUC_RT"));
                        dto.setOpnprcIdx(getString(item, "OPNPRC_IDX"));
                        dto.setHgprcIdx(getString(item, "HGPRC_IDX"));
                        dto.setLwprcIdx(getString(item, "LWPRC_IDX"));
                        dto.setAccTrdvol(getString(item, "ACC_TRDVOL"));
                        dto.setAccTrdval(getString(item, "ACC_TRDVAL"));
                        dto.setMktcap(getString(item, "MKTCAP"));
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
    
    // 최근 트레이딩 날짜 가져오기 
    private LocalDate getLastTradingDay() {
	
	LocalDate date = LocalDate.now();
	
	// 일요일이면 금요일로 조정 
	if(date.getDayOfWeek() == DayOfWeek.SUNDAY) {
	    return date.minusDays(2);
	}
	
	// 토요일이면 금요일로 조정 
	if(date.getDayOfWeek() == DayOfWeek.SATURDAY) {
	    return date.minusDays(1);
	}
	
	// 평일이면 하루전으로 
	return date.minusDays(1);
	
    }
    
    // http 요청 생성 
    

}
