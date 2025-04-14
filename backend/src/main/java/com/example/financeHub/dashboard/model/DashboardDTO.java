package com.example.financeHub.dashboard.model;
import java.util.List;

import com.example.financeHub.crawler.model.CrawledIpoDTO;
import com.example.financeHub.crawler.model.CrawledNewsDTO;

import lombok.Data;

@Data
public class DashboardDTO {

    private List<CrawledNewsDTO> crawledNewsList;
    private List<CrawledIpoDTO> crawledIpoList;
    private List<GoldMarketDailyTradingDTO> goldMarketDailyTradingList;
    private List<OilMarketDailyTradingDTO> oilMarketDailyTradingList;
    private List<KospiDailyTradingDTO> kospiDailyTradingList;
    private List<KosdaqDailyTradingDTO> kosdaqDailyTradingList;
}
