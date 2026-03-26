package com.example.financeHub.overseas.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

import lombok.Data;

/**
 * KIS HHDFS00000300 — 해외주식 현재가 (환율 포함)
 */
@Data
public class OverseasCurrentPriceDTO {

    private String isuCd;       // 티커 (AAPL)
    private String isuNm;       // 종목명
    private String excd;        // 거래소 (NAS/NYS/AMS)
    private String curr;        // 통화 (USD)

    private BigDecimal lastPrc; // 현재가 (USD)
    private BigDecimal basePrc; // 전일종가 (USD)
    private BigDecimal diff;    // 전일대비 (USD)
    private BigDecimal rate;    // 등락률 (%)
    private String    sign;     // 대비구분 (2:상승, 3:보합, 5:하락)

    private BigDecimal xrat;    // 환율 (1 USD = N KRW)
    private BigDecimal krwPrc;  // KRW 환산 현재가

    private Long tvol;          // 거래량

    /** lastPrc * xrat 계산 */
    public void calcKrwPrice() {
        if (lastPrc != null && xrat != null && xrat.compareTo(BigDecimal.ZERO) > 0) {
            krwPrc = lastPrc.multiply(xrat).setScale(0, RoundingMode.HALF_UP);
        }
    }
}
