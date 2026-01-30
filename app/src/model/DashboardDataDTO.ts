
export interface EconomicIndicatorsDTO {
    label: string;  // 이름
    value: string;  // 수치
    change: string;     // 변동량
    isPositive: boolean;    // 음양
}

export interface TopGainersDTO {
    rank: number;   // 등수
    name: string;   // 이름
    price: string;  // 가격
    change: string;     // 변동량
    volume: string; // 거래량량
}

export interface CrawledNewsDTO {
    title: string; // 제목
    link: string; // 링크
}

export interface CrawledIpoDTO {
    companyName: string; // 종목명
    link: string; // 링크
    period: string; // 공모주 일정
    fixedOfferingPrice: string; // 확정공모가
    expectedOfferingPrice: string; // 희망공모가
    subscriptionRate: string; // 청약경쟁률
    underWriter: string; // 주간사
}

/** ✅ 금시장 일별 매매 정보 DTO */
export interface GoldMarketTradingDTO {
    basDd: string;  // 기준일자
    isuCd: string;  // 종목코드
    isuNm: string;  // 종목명
    tddClsprc: string;  // 종가
    cmpprevddPrc: string;  // 대비
    flucRt: string;  // 등락률
    tddOpnprc: string;  // 시가
    tddHgprc: string;  // 고가
    tddLwprc: string;  // 저가
    accTrdvol: string;  // 거래량
    accTrdval: string;  // 거래대금
}

export interface OilMarketDailtyTradingDTO {
    basDd: string;  // 기준일자
    oilNm: string;  // 유종구분
    wtAvgPrc: string;  // 가중평균가격_경쟁
    wtDisAvgPrc: string;  // 가중평균가격_협의
    accTrdvol: string;  // 거래량
    accTrdVal: string;  // 거래대금
}

export interface KospiDailyTradingDTO {
    basDd: string;  // 기준일자
    idxClss: string;  // 계열구분
    idxNm: string;  // 지수명
    clsprcIdx: string;  // 종가
    cmpprevddIdx: string;  // 대비
    flucRt: string;  // 등락률
    opnprcIdx: string;  // 시가
    hgprcIdx: string;  // 고가
    lwprcIdx: string;  // 저가
    accTrdvol: string;  // 거래량
    accTrdval: string;  // 거래대금
    mktcap: string;  // 상장시가총액
}

export interface StockDailyTradingDTO {
    basDd: string;  // 기준일자
    isuCd: string;  // 종목코드
    isuSrtCd: string;  // 종목 단축코드
    isuNm: string;  // 종목명
    mktNm: string;  // 시장구분
    sectTpNm: string;  // 소속부
    tddClsprc: string;  // 종가
    cmpprevddPrc: string;  // 전일대비
    flucRt: string;  // 등락률
    tddOpnprc: string;  // 시가
    tddHgprc: string;  // 고가
    tddLwprc: string;  // 저가
    accTrdvol: string;  // 거래량
    accTrdval: string;  // 거래대금
    mktcap: string;  // 시가총액
    listShrs: string;  // 상장주수
}

export interface DashboardDataDTO {
    economicIndicators: EconomicIndicatorsDTO[];
    topGainers: TopGainersDTO[];
    crawledNewsList: CrawledNewsDTO[];
    crawledIpoList: CrawledIpoDTO[];
    goldMarketDailyTradingList: GoldMarketTradingDTO[];
    oilMarketDailyTradingList: OilMarketDailtyTradingDTO[];
    kospiDailyTradingList: KospiDailyTradingDTO[];
    topGainersList: StockDailyTradingDTO[];
    topVolumeList: StockDailyTradingDTO[];
}


