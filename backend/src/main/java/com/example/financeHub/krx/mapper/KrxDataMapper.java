package com.example.financeHub.krx.mapper;

import java.util.List;
import java.util.Set;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.financeHub.krx.model.GoldMarketDailyTradingDTO;
import com.example.financeHub.krx.model.KosdaqDailyTradingDTO;
import com.example.financeHub.krx.model.KospiDailyTradingDTO;
import com.example.financeHub.krx.model.OilMarketDailyTradingDTO;

@Mapper
public interface KrxDataMapper {

    // KOSPI
    void batchInsertKospi(@Param("list") List<KospiDailyTradingDTO> list);
    Set<String> findExistingKospiHashes(@Param("hashes") Set<String> hashes);

    // KOSDAQ
    void batchInsertKosdaq(@Param("list") List<KosdaqDailyTradingDTO> list);
    Set<String> findExistingKosdaqHashes(@Param("hashes") Set<String> hashes);

    // Gold Market
    void batchInsertGoldMarket(@Param("list") List<GoldMarketDailyTradingDTO> list);
    Set<String> findExistingGoldMarketHashes(@Param("hashes") Set<String> hashes);

    // Oil Market
    void batchInsertOilMarket(@Param("list") List<OilMarketDailyTradingDTO> list);
    Set<String> findExistingOilMarketHashes(@Param("hashes") Set<String> hashes);
}
