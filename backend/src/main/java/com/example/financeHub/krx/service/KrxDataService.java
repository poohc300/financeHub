package com.example.financeHub.krx.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.financeHub.krx.fetcher.GoldMarketFetcher;
import com.example.financeHub.krx.fetcher.KosdaqFetcher;
import com.example.financeHub.krx.fetcher.KospiFetcher;
import com.example.financeHub.krx.fetcher.OilMarketFetcher;
import com.example.financeHub.krx.fetcher.StockFetcher;
import com.example.financeHub.krx.model.GoldMarketDailyTradingDTO;
import com.example.financeHub.krx.model.KosdaqDailyTradingDTO;
import com.example.financeHub.krx.model.KospiDailyTradingDTO;
import com.example.financeHub.krx.model.OilMarketDailyTradingDTO;
import com.example.financeHub.krx.model.StockDailyTradingDTO;


@Service
public class KrxDataService {

    private GoldMarketFetcher goldMarketFetcher;
    private OilMarketFetcher oilMarketFetcher;
    private KospiFetcher kospiFetcher;
    private KosdaqFetcher kosdaqFetcher;
    private StockFetcher stockFetcher;

    public KrxDataService(
            GoldMarketFetcher goldMarketFetcher,
            OilMarketFetcher oilMarketFetcher,
            KospiFetcher kospiFetcher,
            KosdaqFetcher kosdaqFetcher,
            StockFetcher stockFetcher
            ) {
        this.goldMarketFetcher = goldMarketFetcher;
        this.oilMarketFetcher = oilMarketFetcher;
        this.kospiFetcher = kospiFetcher;
        this.kosdaqFetcher = kosdaqFetcher;
        this.stockFetcher = stockFetcher;
    }
    
    
    public List<GoldMarketDailyTradingDTO> getGoldMarketDailyTradingInfo() {
	List<GoldMarketDailyTradingDTO> result = new ArrayList<>();
	
	result.addAll(goldMarketFetcher.fetch());
	
	return result;
    }
    
    public List<OilMarketDailyTradingDTO> getOilMarketDailyTradingInfo() {
	List<OilMarketDailyTradingDTO> result = new ArrayList<>();
	
	result.addAll(oilMarketFetcher.fetch());
	
	return result;
    }
    
    public List<KospiDailyTradingDTO> getKospiDailyTradingInfo() {
	List<KospiDailyTradingDTO> result = new ArrayList<>();
	
	result.addAll(kospiFetcher.fetch());
	
	return result;
    }
    
    public List<KosdaqDailyTradingDTO> getKosdaqDailyTradingInfo() {
        List<KosdaqDailyTradingDTO> result = new ArrayList<>();

        result.addAll(kosdaqFetcher.fetch());

        return result;
    }

    public List<StockDailyTradingDTO> getStockDailyTradingInfo() {
        List<StockDailyTradingDTO> result = new ArrayList<>();

        result.addAll(stockFetcher.fetch());

        return result;
    }
}
