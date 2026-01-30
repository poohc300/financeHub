package com.example.financeHub.krx.model;

import com.example.financeHub.config.HashUtil;

import lombok.Data;

/**
 * 개별 주식 종목 일별 시세 정보
 */
@Data
public class StockDailyTradingDTO {

    /**
     * 기준일자
     */
    private String basDd;

    /**
     * 종목코드
     */
    private String isuCd;

    /**
     * 종목 단축코드
     */
    private String isuSrtCd;

    /**
     * 종목명
     */
    private String isuNm;

    /**
     * 시장구분 (KOSPI/KOSDAQ)
     */
    private String mktNm;

    /**
     * 소속부
     */
    private String sectTpNm;

    /**
     * 종가
     */
    private String tddClsprc;

    /**
     * 전일대비
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

    /**
     * 시가총액
     */
    private String mktcap;

    /**
     * 상장주수
     */
    private String listShrs;

    /**
     * 데이터 해시
     */
    private String dataHash;

    // 해시 생성
    public String generateHash() {
        String key = basDd + "|" + isuCd + "|" + tddClsprc;
        return HashUtil.generateHash(key);
    }
}
