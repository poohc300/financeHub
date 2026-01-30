package com.example.financeHub.krx.mapper;

import java.util.List;
import java.util.Set;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.financeHub.krx.model.GoldMarketDailyTradingDTO;
import com.example.financeHub.krx.model.KosdaqDailyTradingDTO;
import com.example.financeHub.krx.model.KospiDailyTradingDTO;
import com.example.financeHub.krx.model.OilMarketDailyTradingDTO;
import com.example.financeHub.krx.model.StockDailyTradingDTO;

@Mapper
public interface KrxDataMapper {

    // KOSPI
    void batchInsertKospi(@Param("list") List<KospiDailyTradingDTO> list);
    Set<String> findExistingKospiHashes(@Param("hashes") Set<String> hashes);
    List<KospiDailyTradingDTO> selectLatestKospi();
    List<KospiDailyTradingDTO> selectKospiHistory(@Param("indexName") String indexName, @Param("limit") int limit);

    // KOSDAQ
    void batchInsertKosdaq(@Param("list") List<KosdaqDailyTradingDTO> list);
    Set<String> findExistingKosdaqHashes(@Param("hashes") Set<String> hashes);
    List<KosdaqDailyTradingDTO> selectLatestKosdaq();
    List<KosdaqDailyTradingDTO> selectKosdaqHistory(@Param("indexName") String indexName, @Param("limit") int limit);

    // Gold Market
    void batchInsertGoldMarket(@Param("list") List<GoldMarketDailyTradingDTO> list);
    Set<String> findExistingGoldMarketHashes(@Param("hashes") Set<String> hashes);
    List<GoldMarketDailyTradingDTO> selectLatestGoldMarket();

    // Oil Market
    void batchInsertOilMarket(@Param("list") List<OilMarketDailyTradingDTO> list);
    Set<String> findExistingOilMarketHashes(@Param("hashes") Set<String> hashes);
    List<OilMarketDailyTradingDTO> selectLatestOilMarket();

    // Stock (개별 종목)
    void batchInsertStock(@Param("list") List<StockDailyTradingDTO> list);
    Set<String> findExistingStockHashes(@Param("hashes") Set<String> hashes);
    List<StockDailyTradingDTO> selectTopGainers(@Param("limit") int limit);
    List<StockDailyTradingDTO> selectTopVolume(@Param("limit") int limit);
}
