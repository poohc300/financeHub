
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


export interface DashboardDataDTO {
    economicIndicators: EconomicIndicatorsDTO[];
    topGainers: TopGainersDTO[];
    crawledNewsList: CrawledNewsDTO[];
    crawledIpoList: CrawledIpoDTO[];
    goldMarketTradingData: GoldMarketTradingDTO[];  // 추가된 필드

}


