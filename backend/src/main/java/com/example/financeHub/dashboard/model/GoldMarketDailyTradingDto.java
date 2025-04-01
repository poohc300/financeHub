package com.example.financeHub.dashboard.model;
import lombok.Data;

/**
 * 금시장 일별매매정보 DTO
 * OutBlock_1 데이터 구조에 맞게 설계됨
 */
@Data
public class GoldMarketDailyTradingDto {
    
    /**
     * 기준일자
     */
    private String basDd;
    
    /**
     * 종목코드
     */
    private String isuCd;
    
    /**
     * 종목명
     */
    private String isuNm;
    
    /**
     * 종가
     */
    private String tddClsprc;
    
    /**
     * 대비
     */
    private String cmpprevddPrc;
    
    /**
     * 등락률
     */
    private String flucRt;
    
    /**
     * 시가
     */
    private String tddOpnprc;
    
    /**
     * 고가
     */
    private String tddHgprc;
    
    /**
     * 저가
     */
    private String tddLwprc;
    
    /**
     * 거래량
     */
    private String accTrdvol;
    
    /**
     * 거래대금
     */
    private String accTrdval;
}