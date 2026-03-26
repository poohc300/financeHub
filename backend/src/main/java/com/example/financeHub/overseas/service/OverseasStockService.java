package com.example.financeHub.overseas.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.financeHub.overseas.fetcher.OverseasStockFetcher;
import com.example.financeHub.overseas.model.OverseasStockDailyTradingDTO;
import com.example.financeHub.overseas.util.OverseasMarketUtil;

@Service
public class OverseasStockService {

    private final OverseasStockFetcher fetcher;

    public OverseasStockService(OverseasStockFetcher fetcher) {
        this.fetcher = fetcher;
    }

    /**
     * 워치리스트 전체 수집 (스케줄러용)
     * bassDt: YYYYMMDD, null 시 전날 기준
     */
    public List<OverseasStockDailyTradingDTO> getWatchlistData(String bassDt) {
        String targetDate = (bassDt != null) ? bassDt : OverseasMarketUtil.getCollectionDate();
        return fetcher.fetchWatchlist(targetDate);
    }

    /**
     * 특정 종목 일별 시세 (차트/검색용)
     */
    public List<OverseasStockDailyTradingDTO> getDailyPriceList(String excd, String symb, String name) {
        return fetcher.fetchDailyPriceList(excd, symb, name);
    }
}
