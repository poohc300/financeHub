package com.example.financeHub.krx.fetcher;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.financeHub.krx.model.OilMarketDailyTradingDTO;
import com.example.financeHub.krx.util.KrxApiCaller;
import com.example.financeHub.krx.util.KrxCommonUtil;

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
	    LocalDate date = KrxCommonUtil.getLastTradingDay();
	    String formattedDate = KrxCommonUtil.formatDate(date);
	    
	    List<Map<String, Object>> dataList = krxApiCaller.callApi(oilUrl, formattedDate);
	    
	    if(!dataList.isEmpty()) {
		for(Map<String, Object> item : dataList) {
		    OilMarketDailyTradingDTO dto = new OilMarketDailyTradingDTO();
                    dto.setBasDd(KrxCommonUtil.getString(item, "BAS_DD"));
                    dto.setOilNm(KrxCommonUtil.getString(item, "OIL_NM"));
                    dto.setWtAvgPrc(KrxCommonUtil.getString(item, "WT_AVG_PRC"));
                    dto.setWtDisAvgPrc(KrxCommonUtil.getString(item, "WT_DIS_AVG_PRC"));
                    dto.setAccTrdvol(KrxCommonUtil.getString(item, "ACC_TRDVOL"));
                    dto.setAccTrdVal(KrxCommonUtil.getString(item, "ACC_TRDVAL"));
                    result.add(dto);
		}
	    }
	} catch (Exception e) {
            System.err.println("API 호출 중 오류 발생: " + e.getMessage());
	}
	return result;
    }
}
