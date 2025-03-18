
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

export interface DashboardDataDTO {
    economicIndicators: EconomicIndicatorsDTO[];
    topGainers: TopGainersDTO[];
    crawledNewsList: CrawledNewsDTO[];
    crawledIpoList: CrawledIpoDTO[];
}


