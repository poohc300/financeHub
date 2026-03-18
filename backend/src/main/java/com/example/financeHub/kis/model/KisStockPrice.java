package com.example.financeHub.kis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KisStockPrice {
    private String isuSrtCd;    // 단축종목코드 (예: 005930)
    private String currentPrice; // 현재가
    private String change;       // 전일대비
    private String changeRate;   // 등락률
    private String volume;       // 누적거래량
    private String time;         // 체결시간 (HHMMSS)
    private String open;         // 시가
    private String high;         // 고가
    private String low;          // 저가
}
