package com.example.financeHub.krx.fetcher;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.financeHub.krx.model.KospiDailyTradingDTO;
import com.example.financeHub.krx.util.KrxApiCaller;

@Service
public class KospiFetcher {

    private final String kospiUrl = "idx/kospi_dd_trd";
    private final KrxApiCaller krxApiCaller;

    public KospiFetcher(KrxApiCaller krxApiCaller) {
	// TODO Auto-generated constructor stub
	this.krxApiCaller = krxApiCaller;
    }
    
    public List<KospiDailyTradingDTO> fetch() {
	
	List<KospiDailyTradingDTO> result = new ArrayList<>();
	
	try {
	    
	} catch (Exception e) {
	    // TODO: handle exception
	}
	
	return result;
    }
}
