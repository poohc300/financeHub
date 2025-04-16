package com.example.financeHub.krx.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.financeHub.krx.fetcher.GoldMarketFetcher;
import com.example.financeHub.krx.fetcher.KosdaqFetcher;
import com.example.financeHub.krx.fetcher.KospiFetcher;
import com.example.financeHub.krx.fetcher.OilMarketFetcher;
import com.example.financeHub.krx.model.GoldMarketDailyTradingDTO;
import com.example.financeHub.krx.model.KosdaqDailyTradingDTO;
import com.example.financeHub.krx.model.KospiDailyTradingDTO;
import com.example.financeHub.krx.model.OilMarketDailyTradingDTO;


@Service
public class KrxDataService {
   
    private GoldMarketFetcher goldMarketFetcher;
    private OilMarketFetcher oilMarketFetcher;
    private KospiFetcher kospiFetcher;
    private KosdaqFetcher kosdaqFetcher;
    
    public KrxDataService(
	    GoldMarketFetcher goldMarketFetcher,
	    OilMarketFetcher oilMarketFetcher,
	    KospiFetcher kospiFetcher,
	    KosdaqFetcher kosdaqFetcher
	    ) {
	this.goldMarketFetcher = goldMarketFetcher;
	this.oilMarketFetcher = oilMarketFetcher;
	this.kospiFetcher = kospiFetcher;
	this.kosdaqFetcher = kosdaqFetcher;
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
}
