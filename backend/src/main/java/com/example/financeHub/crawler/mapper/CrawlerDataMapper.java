package com.example.financeHub.crawler.mapper;

import java.util.List;
import java.util.Set;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.financeHub.crawler.ipo.IpoDTO;
import com.example.financeHub.crawler.news.NewsDTO;
import com.example.financeHub.scheduler.model.SchedulerExecutionLogDTO;

@Mapper
public interface CrawlerDataMapper {

    // News
    void batchInsertNews(@Param("list") List<NewsDTO> list);
    Set<String> findExistingNewsHashes(@Param("hashes") Set<String> hashes);

    // IPO
    void batchInsertIpo(@Param("list") List<IpoDTO> list);
    Set<String> findExistingIpoHashes(@Param("hashes") Set<String> hashes);

    // Scheduler Execution Log
    void insertExecutionLog(SchedulerExecutionLogDTO log);
}
