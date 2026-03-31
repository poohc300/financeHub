package com.example.financeHub.overseas.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Overseas52WeekDTO {

    private String isuCd;
    private String isuNm;
    private String excd;
    private BigDecimal curPrc;
    private BigDecimal high52w;
    private BigDecimal low52w;
    private BigDecimal fromHighPct;  // (curPrc - high52w) / high52w * 100
    private BigDecimal fromLowPct;   // (curPrc - low52w)  / low52w  * 100
    private BigDecimal rangePct;     // 52주 범위 내 현재 위치 0~100%

    public void calcPct() {
        if (high52w != null && curPrc != null && high52w.compareTo(BigDecimal.ZERO) != 0) {
            fromHighPct = curPrc.subtract(high52w)
                .divide(high52w, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
        }
        if (low52w != null && curPrc != null && low52w.compareTo(BigDecimal.ZERO) != 0) {
            fromLowPct = curPrc.subtract(low52w)
                .divide(low52w, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
        }
        if (high52w != null && low52w != null && curPrc != null) {
            BigDecimal range = high52w.subtract(low52w);
            if (range.compareTo(BigDecimal.ZERO) != 0) {
                rangePct = curPrc.subtract(low52w)
                    .divide(range, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .min(new BigDecimal("100"))
                    .max(BigDecimal.ZERO)
                    .setScale(1, RoundingMode.HALF_UP);
            }
        }
    }

    public String getIsuCd() { return isuCd; }
    public void setIsuCd(String isuCd) { this.isuCd = isuCd; }
    public String getIsuNm() { return isuNm; }
    public void setIsuNm(String isuNm) { this.isuNm = isuNm; }
    public String getExcd() { return excd; }
    public void setExcd(String excd) { this.excd = excd; }
    public BigDecimal getCurPrc() { return curPrc; }
    public void setCurPrc(BigDecimal curPrc) { this.curPrc = curPrc; }
    public BigDecimal getHigh52w() { return high52w; }
    public void setHigh52w(BigDecimal high52w) { this.high52w = high52w; }
    public BigDecimal getLow52w() { return low52w; }
    public void setLow52w(BigDecimal low52w) { this.low52w = low52w; }
    public BigDecimal getFromHighPct() { return fromHighPct; }
    public void setFromHighPct(BigDecimal fromHighPct) { this.fromHighPct = fromHighPct; }
    public BigDecimal getFromLowPct() { return fromLowPct; }
    public void setFromLowPct(BigDecimal fromLowPct) { this.fromLowPct = fromLowPct; }
    public BigDecimal getRangePct() { return rangePct; }
    public void setRangePct(BigDecimal rangePct) { this.rangePct = rangePct; }
}
