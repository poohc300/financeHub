package com.example.financeHub.kis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KisRankingItem {
    private String isuSrtCd;   // 단축종목코드
    private String isuNm;      // 종목명
    private String currentPrice; // 현재가
    private String change;       // 전일대비
    private String changeRate;   // 등락률
    private String volume;       // 누적거래량
}
