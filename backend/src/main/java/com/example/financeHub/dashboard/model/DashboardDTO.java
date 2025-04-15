package com.example.financeHub.dashboard.model;
import java.util.List;

import com.example.financeHub.crawler.ipo.IpoDTO;
import com.example.financeHub.crawler.news.NewsDTO;
import com.example.financeHub.krx.model.GoldMarketDailyTradingDTO;
import com.example.financeHub.krx.model.KosdaqDailyTradingDTO;
import com.example.financeHub.krx.model.KospiDailyTradingDTO;
import com.example.financeHub.krx.model.OilMarketDailyTradingDTO;

import lombok.Data;

@Data
public class DashboardDTO {

    private List<NewsDTO> crawledNewsList;
    private List<IpoDTO> crawledIpoList;
    private List<GoldMarketDailyTradingDTO> goldMarketDailyTradingList;
    private List<OilMarketDailyTradingDTO> oilMarketDailyTradingList;
    private List<KospiDailyTradingDTO> kospiDailyTradingList;
    private List<KosdaqDailyTradingDTO> kosdaqDailyTradingList;
}
