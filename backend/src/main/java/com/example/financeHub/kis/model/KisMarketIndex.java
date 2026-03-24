package com.example.financeHub.kis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KisMarketIndex {
    private String idxNm;       // 지수명 (코스피/코스닥)
    private String currentIdx;  // 현재지수
    private String change;      // 전일대비
    private String changeRate;  // 등락률
    private String open;        // 시가지수
    private String high;        // 고가지수
    private String low;         // 저가지수
    private String volume;      // 누적거래량
}
