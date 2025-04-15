package com.example.financeHub.krx.fetcher;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.financeHub.krx.model.OilMarketDailyTradingDTO;
import com.example.financeHub.krx.util.KrxApiCaller;

@Service
public class OilMarketFetcher {

    private final String oilUrl = "gen/oil_bydd_trd";
    private final KrxApiCaller krxApiCaller;

    
    public OilMarketFetcher(KrxApiCaller krxApiCaller) {
	this.krxApiCaller = krxApiCaller;
    }
    
    public List<OilMarketDailyTradingDTO> fetch() {
	List<OilMarketDailyTradingDTO> result = new ArrayList<>();
	
	try {
	    
	} catch (Exception e) {
	    // TODO: handle exception
	}
	return result;
    }
}
