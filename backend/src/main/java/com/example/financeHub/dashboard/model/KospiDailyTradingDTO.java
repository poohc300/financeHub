package com.example.financeHub.dashboard.model;

import lombok.Data;
/**
 * kospi 시리즈 일별 시세 정보 
 */
@Data
public class KospiDailyTradingDTO {

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
}
