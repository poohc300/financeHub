package com.example.financeHub.overseas.model;

import java.math.BigDecimal;

import com.example.financeHub.config.HashUtil;

import lombok.Data;

/**
 * 해외주식 일별 시세 정보
 */
@Data
public class OverseasStockDailyTradingDTO {

    private Long id;

    /** 기준일자 YYYYMMDD */
    private String bassDt;

    /** 종목코드 (ticker) */
    private String isuCd;

    /** 종목명 */
    private String isuNm;

    /** 거래소코드 (NAS/NYS/AMS) */
    private String excd;

    /** 통화 */
    private String curr;

    /** 종가 */
    private BigDecimal clsPrc;

    /** 시가 */
    private BigDecimal opnPrc;

    /** 고가 */
    private BigDecimal hgstPrc;

    /** 저가 */
    private BigDecimal lwstPrc;

    /** 거래량 */
    private Long accTrdVol;

    /** 등락률 */
    private BigDecimal flucRt;

    private String dataHash;

    public String generateHash() {
        String key = bassDt + "|" + isuCd + "|" + excd + "|" + (clsPrc != null ? clsPrc.toPlainString() : "0");
        return HashUtil.generateHash(key);
    }
}
