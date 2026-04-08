package com.example.financeHub.kis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KisFuturesPrice {
    private String isuCd;        // 종목코드 (e.g. 101W2606)
    private String isuNm;        // 종목명 (e.g. K200선물 6월)
    private String time;         // 체결시간
    private String currentPrice; // 현재가 (소수점 포함, e.g. "327.15")
    private String change;       // 전일대비
    private String changeRate;   // 등락률 (%)
    private String open;         // 시가
    private String high;         // 고가
    private String low;          // 저가
    private String volume;       // 누적체결량
}
