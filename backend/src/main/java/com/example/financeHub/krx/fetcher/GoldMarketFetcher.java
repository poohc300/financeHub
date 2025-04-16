package com.example.financeHub.krx.fetcher;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import com.example.financeHub.krx.model.GoldMarketDailyTradingDTO;
import com.example.financeHub.krx.util.KrxApiCaller;
import com.example.financeHub.krx.util.KrxCommonUtil;

@Service
public class GoldMarketFetcher {

    private final String goldUrl = "gen/gold_bydd_trd";
    private final KrxApiCaller krxApiCaller;
    
    public GoldMarketFetcher(KrxApiCaller krxApiCaller) {
	this.krxApiCaller = krxApiCaller;
    }
    
    public List<GoldMarketDailyTradingDTO> fetch() {
	List<GoldMarketDailyTradingDTO> result = new ArrayList<>();
	
	try {
	    LocalDate date = KrxCommonUtil.getLastTradingDay();
	    String formattedDate = KrxCommonUtil.formatDate(date);
	    
	    List<Map<String, Object>> dataList = krxApiCaller.callApi(goldUrl, formattedDate);
	    
	    if(!dataList.isEmpty()) {
		for(Map<String, Object> item: dataList) {
		    GoldMarketDailyTradingDTO dto = new GoldMarketDailyTradingDTO();
                    dto.setBasDd(KrxCommonUtil.getString(item, "BAS_DD"));
                    dto.setIsuCd(KrxCommonUtil.getString(item, "ISU_CD"));
                    dto.setIsuNm(KrxCommonUtil.getString(item, "ISU_NM"));
                    dto.setTddClsprc(KrxCommonUtil.getString(item, "TDD_CLSPRC"));
                    dto.setCmpprevddPrc(KrxCommonUtil.getString(item, "CMPPREVDD_PRC"));
                    dto.setFlucRt(KrxCommonUtil.getString(item, "FLUC_RT"));
                    dto.setTddOpnprc(KrxCommonUtil.getString(item, "TDD_OPNPRC"));
                    dto.setTddHgprc(KrxCommonUtil.getString(item, "TDD_HGPRC"));
                    dto.setTddLwprc(KrxCommonUtil.getString(item, "TDD_LWPRC"));
                    dto.setAccTrdvol(KrxCommonUtil.getString(item, "ACC_TRDVOL"));
                    dto.setAccTrdval(KrxCommonUtil.getString(item, "ACC_TRDVAL"));
                    result.add(dto);
		}
	    }
	} catch (Exception e) {
            System.err.println("API 호출 중 오류 발생: " + e.getMessage());
	}
	return result;
    }
}
