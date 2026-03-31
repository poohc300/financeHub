package com.example.financeHub.overseas.mapper;

import java.util.List;
import java.util.Set;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.financeHub.overseas.model.Overseas52WeekDTO;
import com.example.financeHub.overseas.model.OverseasStockDailyTradingDTO;

@Mapper
public interface OverseasStockMapper {

    void batchInsert(@Param("list") List<OverseasStockDailyTradingDTO> list);

    Set<String> findExistingHashes(@Param("hashes") Set<String> hashes);

    List<OverseasStockDailyTradingDTO> searchStocks(@Param("keyword") String keyword,
                                                     @Param("limit") int limit);

    List<OverseasStockDailyTradingDTO> selectHistory(@Param("isuCd") String isuCd,
                                                      @Param("excd") String excd,
                                                      @Param("limit") int limit);

    List<OverseasStockDailyTradingDTO> selectHistoryByDateRange(@Param("isuCd") String isuCd,
                                                                 @Param("excd") String excd,
                                                                 @Param("startDate") String startDate,
                                                                 @Param("endDate") String endDate);

    List<Overseas52WeekDTO> select52WeekStats();
}
