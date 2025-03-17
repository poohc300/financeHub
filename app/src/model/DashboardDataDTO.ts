
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

export interface MarketEventsDTO {
    title: string; // 제목
    link: string; // 링크
}

export interface EconomicEventsDTO {
    title: string; // 제목
    link: string; // 링크
}

export interface DashboardDataDTO {
    economicIndicators: EconomicIndicatorsDTO[];
    topGainers: TopGainersDTO[];
    marketEvents: MarketEventsDTO[];
    economicEvents: EconomicEventsDTO[];
}


