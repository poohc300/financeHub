package com.example.financeHub.krx.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.financeHub.krx.mapper.KrxDataMapper;
import com.example.financeHub.krx.model.Domestic52WeekDTO;

@RestController
@RequestMapping("/stocks")
public class StockInsightController {

    private final KrxDataMapper krxDataMapper;

    public StockInsightController(KrxDataMapper krxDataMapper) {
        this.krxDataMapper = krxDataMapper;
    }

    /**
     * 국내 주식 52주 신고가/저가 대비 현재가 통계
     * GET /stocks/52week
     */
    @GetMapping("/52week")
    public ResponseEntity<List<Domestic52WeekDTO>> weekStats52() {
        List<Domestic52WeekDTO> result = krxDataMapper.select52WeekStats();
        result.forEach(Domestic52WeekDTO::calcPct);
        return ResponseEntity.ok(result);
    }
}
