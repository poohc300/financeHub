package com.example.financeHub.krx.fetcher;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.financeHub.krx.model.KospiDailyTradingDTO;
import com.example.financeHub.krx.util.KrxApiCaller;
import com.example.financeHub.krx.util.KrxCommonUtil;

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
	    LocalDate date = KrxCommonUtil.getLastTradingDay();
	    String formattedDate = KrxCommonUtil.formatDate(date);
	    
	    List<Map<String, Object>> dataList = krxApiCaller.callApi(kospiUrl, formattedDate);
	    
	    if(!dataList.isEmpty()) {
		for(Map<String, Object> item: dataList) {
		    KospiDailyTradingDTO dto = new KospiDailyTradingDTO();
		    dto.setBasDd(KrxCommonUtil.getString(item, "BAS_DD"));
                    dto.setIdxClss(KrxCommonUtil.getString(item, "IDX_CLSS"));
                    dto.setIdxNm(KrxCommonUtil.getString(item, "IDX_NM"));
                    dto.setClsprcIdx(KrxCommonUtil.getString(item, "CLSPRC_IDX"));
                    dto.setCmpprevddIdx(KrxCommonUtil.getString(item, "CMPPREVDD_IDX"));
                    dto.setFlucRt(KrxCommonUtil.getString(item, "FLUC_RT"));
                    dto.setOpnprcIdx(KrxCommonUtil.getString(item, "OPNPRC_IDX"));
                    dto.setHgprcIdx(KrxCommonUtil.getString(item, "HGPRC_IDX"));
                    dto.setLwprcIdx(KrxCommonUtil.getString(item, "LWPRC_IDX"));
                    dto.setAccTrdvol(KrxCommonUtil.getString(item, "ACC_TRDVOL"));
                    dto.setAccTrdval(KrxCommonUtil.getString(item, "ACC_TRDVAL"));
                    dto.setMktcap(KrxCommonUtil.getString(item, "MKTCAP"));
                    result.add(dto);
		}
	    }
	} catch (Exception e) {
	    // TODO: handle exception
            System.err.println("API 호출 중 오류 발생: " + e.getMessage());
	}
	
	return result;
    }
}
