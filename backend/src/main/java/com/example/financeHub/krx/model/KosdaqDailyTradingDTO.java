package com.example.financeHub.krx.model;

import com.example.financeHub.config.HashUtil;

import lombok.Data;
/**
 * kosdaq 시리즈 지수의 시세 정보 
 */
@Data
public class KosdaqDailyTradingDTO {

    /**
     * 기준일자
     */
    private String basDd;
    
    /**
     * 계열구분
     */
    private String idxClss;
    
    /**
     * 지수명
     */
    private String idxNm;
    
    /**
     * 종가
     */
    private String clsprcIdx;
    
    /**
     * 대비
     */
    private String cmpprevddIdx;
    
    /**
     * 등락률
     */
    private String flucRt;
    
    /**
     * 시가
     */
    private String opnprcIdx;
    
    /**
     * 고가
     */
    private String hgprcIdx;
    
    /**
     * 저가
     */
    private String lwprcIdx;
    
    /**
     * 거래량
     */
    private String accTrdvol;
    
    /**
     * 거래대금
     */
    private String accTrdval;
    
    /**
     * 상장시가총액
     */
    private String mktcap;

    /**
     * 데이터 해시
     */
    private String dataHash;

    // 해시 생성 
    public String generateHash() {
	String key = basDd + "|" + idxNm + "|" + clsprcIdx;
	return HashUtil.generateHash(key);
    }
}
