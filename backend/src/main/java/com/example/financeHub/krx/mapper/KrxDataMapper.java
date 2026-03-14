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
    List<KospiDailyTradingDTO> selectKospiHistoryByDateRange(@Param("indexName") String indexName, @Param("startDate") String startDate, @Param("endDate") String endDate);

    // KOSDAQ
    void batchInsertKosdaq(@Param("list") List<KosdaqDailyTradingDTO> list);
    Set<String> findExistingKosdaqHashes(@Param("hashes") Set<String> hashes);
    List<KosdaqDailyTradingDTO> selectLatestKosdaq();
    List<KosdaqDailyTradingDTO> selectKosdaqHistory(@Param("indexName") String indexName, @Param("limit") int limit);
    List<KosdaqDailyTradingDTO> selectKosdaqHistoryByDateRange(@Param("indexName") String indexName, @Param("startDate") String startDate, @Param("endDate") String endDate);

    // Gold Market
    void batchInsertGoldMarket(@Param("list") List<GoldMarketDailyTradingDTO> list);
    Set<String> findExistingGoldMarketHashes(@Param("hashes") Set<String> hashes);
    List<GoldMarketDailyTradingDTO> selectLatestGoldMarket();
    List<GoldMarketDailyTradingDTO> selectGoldHistory(@Param("isuNm") String isuNm, @Param("limit") int limit);
    List<GoldMarketDailyTradingDTO> selectGoldHistoryByDateRange(@Param("isuNm") String isuNm, @Param("startDate") String startDate, @Param("endDate") String endDate);

    // Oil Market
    void batchInsertOilMarket(@Param("list") List<OilMarketDailyTradingDTO> list);
    Set<String> findExistingOilMarketHashes(@Param("hashes") Set<String> hashes);
    List<OilMarketDailyTradingDTO> selectLatestOilMarket();
    List<OilMarketDailyTradingDTO> selectPrevOilMarket();
    List<OilMarketDailyTradingDTO> selectOilHistory(@Param("oilNm") String oilNm, @Param("limit") int limit);
    List<OilMarketDailyTradingDTO> selectOilHistoryByDateRange(@Param("oilNm") String oilNm, @Param("startDate") String startDate, @Param("endDate") String endDate);

    // Stock (개별 종목)
    void batchInsertStock(@Param("list") List<StockDailyTradingDTO> list);
    Set<String> findExistingStockHashes(@Param("hashes") Set<String> hashes);
    List<StockDailyTradingDTO> selectTopGainers(@Param("limit") int limit);
    List<StockDailyTradingDTO> selectTopVolume(@Param("limit") int limit);
    List<StockDailyTradingDTO> searchStocks(@Param("keyword") String keyword, @Param("limit") int limit);
    List<StockDailyTradingDTO> selectStockHistory(@Param("isuCd") String isuCd, @Param("limit") int limit);
    List<StockDailyTradingDTO> selectStockHistoryByDateRange(@Param("isuCd") String isuCd, @Param("startDate") String startDate, @Param("endDate") String endDate);
}
