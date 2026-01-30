package com.example.financeHub.krx.model;

import com.example.financeHub.config.HashUtil;

import lombok.Data;

/**
 * 석유 시장 일별 매매 정보 DTO
 */
@Data
public class OilMarketDailyTradingDTO {

    /**
     * 기준일자
     */
    private String basDd;
    
    /**
     * 유종구분
     */
    private String oilNm;
    
    /**
     * 가중평균가격_경쟁
     */
    private String wtAvgPrc;
    
    /**
     * 가중평균가격_협의
     */
    private String wtDisAvgPrc;
    
    /**
     * 거래량
     */
    private String accTrdvol;
    
    /**
     * 거래대금
     */
    private String accTrdVal;

    /**
     * 데이터 해시
     */
    private String dataHash;

    // 해시 생성 
    public String generateHash() {
	String key = basDd + "|" + oilNm + "|" + wtAvgPrc;
	return HashUtil.generateHash(key);
    }
}
