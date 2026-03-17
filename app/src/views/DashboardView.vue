<script setup lang="ts">
import { ref, onMounted, computed, watchEffect } from 'vue';
import { CrawledIpoDTO, CrawledNewsDTO, GoldMarketTradingDTO, DashboardDataDTO, EconomicIndicatorsDTO, OilMarketDailtyTradingDTO, KospiDailyTradingDTO, StockDailyTradingDTO } from '../model/DashboardDataDTO';
import { fetchRequest } from '../util/fetchRequest';
import Calendar from '../components/Calendar.vue';
import { CalendarEventDTO } from '../model/CalendarEventDTO';

const economicIndicators = ref<EconomicIndicatorsDTO[]>([]);

const ipoList = ref<CrawledIpoDTO[]>([]);
const newsList = ref<CrawledNewsDTO[]>([]);
const goldMarketInfo = ref<GoldMarketTradingDTO[]>([]);
const oilMarketInfo = ref<OilMarketDailtyTradingDTO[]>([]);
const kospiInfo = ref<KospiDailyTradingDTO[]>([]);
const topGainersList = ref<StockDailyTradingDTO[]>([]);
const topVolumeList = ref<StockDailyTradingDTO[]>([]);

const calendarEvents = computed<CalendarEventDTO[]>(() => {
  if (ipoList.value && ipoList.value.length > 0) {
    return ipoList.value
      .filter(ipo => ipo.period && ipo.period.includes('~'))
      .map(ipo => {
        const parts = ipo.period.split('~');
        const startDate = parts[0].trim();
        const endDate = parts[1].trim();
        const year = startDate.split('.')[0];

        const startFormatted = `${startDate.replace(/\./g, '-')}T00:00:00`;
        const endFormatted = `${year}-${endDate.replace(/\./g, '-')}T23:59:59`;

        return {
          title: ipo.companyName,
          start: startFormatted,
          end: endFormatted,
        };
      });
  }
  return [];
});

/**
 * fetch된 데이터 EconomicIndicators에 변환
 *
 * 1) 금
 * 2) 석유
 * 3) ..
 */
const mapGold = (data: GoldMarketTradingDTO):EconomicIndicatorsDTO => {
  return {
    label: data.isuNm,
    value: data.tddOpnprc,
    change: data.flucRt,
    isPositive: parseFloat(data.flucRt) >= 0,
  };
}

const mapOil = (data: OilMarketDailtyTradingDTO):EconomicIndicatorsDTO => {
  const rate = parseFloat(data.flucRt || '0')
  return {
    label: data.oilNm,
    value: data.wtAvgPrc,
    change: data.flucRt ? `${rate >= 0 ? '+' : ''}${data.flucRt}%` : '',
    isPositive: rate >= 0
  }
}
const mapKospi = (data: KospiDailyTradingDTO):EconomicIndicatorsDTO => {
  return {
    label: data.idxNm,
    value: data.opnprcIdx,
    change: data.flucRt,
    isPositive: parseFloat(data.flucRt) >= 0,
  }
}

// fetch
const fetchDashboardData = async() => {
  try {
    const data = await fetchRequest<DashboardDataDTO>("/dashboard/data", "GET");
    newsList.value = data.crawledNewsList || [];
    ipoList.value = data.crawledIpoList || [];
    goldMarketInfo.value = data.goldMarketDailyTradingList || [];
    oilMarketInfo.value = data.oilMarketDailyTradingList || [];
    kospiInfo.value = data.kospiDailyTradingList || [];
    topGainersList.value = data.topGainersList || [];
    topVolumeList.value = data.topVolumeList || [];

  } catch (error) {
    console.error("Error fetching dashboard data:", error);
    throw error;
  }
}

// 순위 뱃지 색상 (1위=금, 2위=은, 3위=동, 나머지=회색)
const rankBadgeClass = (index: number): string => {
  if (index === 0) return 'bg-yellow-400 text-white';
  if (index === 1) return 'bg-gray-300 text-gray-700';
  if (index === 2) return 'bg-orange-400 text-white';
  return 'bg-gray-100 text-gray-500';
};

// 마켓 뱃지 색상
const marketBadgeClass = (mktNm: string): string => {
  if (mktNm?.includes('코스피') || mktNm?.toUpperCase().includes('KOSPI')) return 'bg-blue-100 text-blue-700';
  if (mktNm?.includes('코스닥') || mktNm?.toUpperCase().includes('KOSDAQ')) return 'bg-purple-100 text-purple-700';
  return 'bg-gray-100 text-gray-600';
};

// 가격 포맷 (1,234 원)
const formatPrice = (val: string): string => {
  if (!val) return '-';
  const n = parseInt(val.replace(/,/g, ''), 10);
  if (isNaN(n)) return val;
  return n.toLocaleString('ko-KR') + '원';
};

// 거래량 포맷 (만/억 단위)
const formatVolume = (val: string): string => {
  if (!val) return '-';
  const n = parseInt(val.replace(/,/g, ''), 10);
  if (isNaN(n)) return val;
  if (n >= 100_000_000) return (n / 100_000_000).toFixed(1) + '억주';
  if (n >= 10_000) return (n / 10_000).toFixed(1) + '만주';
  return n.toLocaleString('ko-KR') + '주';
};

onMounted(() => {
  fetchDashboardData();
})

watchEffect(() => {

  economicIndicators.value = [
    ...goldMarketInfo.value.map(mapGold),
    ...oilMarketInfo.value.map(mapOil),
    ...kospiInfo.value.map(mapKospi),
  ]
});

</script>

<template>
  <div class="max-w-6xl mx-auto">
    
    <!-- 경제 지표 -->
    <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4 mb-8">
      <h1 class="text-xl font-bold text-gray-800 mb-4">주요 경제 지표</h1>

      <div 
        v-for="(indicator, index) in economicIndicators" 
        :key="index"
        class="bg-white rounded-xl shadow-sm p-4 border border-gray-200 hover:shadow-md transition-shadow duration-200"
      >
        <h3 class="text-sm font-medium text-gray-500 mb-1">{{ indicator.label }}</h3>
        <div class="text-lg font-bold text-gray-800 mb-1">{{ indicator.value }}</div>
        <div :class="indicator.isPositive ? 'text-green-600' : 'text-red-600'" class="text-sm font-medium">
          {{ indicator.change }}
        </div>
      </div>
    </div>

    <!-- 상승률 TOP 5 & 거래량 TOP 5 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
      <!-- 상승률 TOP 5 -->
      <div class="bg-white rounded-xl shadow-sm p-6 border border-gray-200">
        <div class="flex items-center gap-2 mb-5">
          <span class="text-xl">🚀</span>
          <h2 class="text-lg font-bold text-gray-800">오늘의 상승률 TOP 5</h2>
        </div>
        <div v-if="topGainersList.length === 0" class="flex flex-col items-center justify-center py-10 text-gray-400">
          <span class="text-3xl mb-2">📭</span>
          <span class="text-sm">데이터가 없습니다</span>
        </div>
        <ul v-else class="space-y-2">
          <li
            v-for="(stock, index) in topGainersList"
            :key="stock.isuCd"
            class="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-50 transition-colors cursor-default"
          >
            <!-- 순위 뱃지 -->
            <span :class="rankBadgeClass(index)" class="w-7 h-7 flex-shrink-0 flex items-center justify-center rounded-full text-xs font-bold">
              {{ index + 1 }}
            </span>
            <!-- 종목 정보 -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-1.5">
                <span class="font-semibold text-gray-800 text-sm truncate">{{ stock.isuNm }}</span>
                <span :class="marketBadgeClass(stock.mktNm)" class="text-[10px] px-1.5 py-0.5 rounded font-medium flex-shrink-0">
                  {{ stock.mktNm }}
                </span>
              </div>
              <div class="text-xs text-gray-400 mt-0.5">{{ stock.isuSrtCd }}</div>
            </div>
            <!-- 가격 & 등락률 -->
            <div class="text-right flex-shrink-0">
              <div class="text-sm font-semibold text-gray-800">{{ formatPrice(stock.tddClsprc) }}</div>
              <div class="flex items-center justify-end gap-0.5 text-green-600">
                <span class="text-xs">▲</span>
                <span class="text-sm font-bold">{{ stock.flucRt }}%</span>
              </div>
            </div>
          </li>
        </ul>
      </div>

      <!-- 거래량 TOP 5 -->
      <div class="bg-white rounded-xl shadow-sm p-6 border border-gray-200">
        <div class="flex items-center gap-2 mb-5">
          <span class="text-xl">🔥</span>
          <h2 class="text-lg font-bold text-gray-800">오늘의 거래량 TOP 5</h2>
        </div>
        <div v-if="topVolumeList.length === 0" class="flex flex-col items-center justify-center py-10 text-gray-400">
          <span class="text-3xl mb-2">📭</span>
          <span class="text-sm">데이터가 없습니다</span>
        </div>
        <ul v-else class="space-y-2">
          <li
            v-for="(stock, index) in topVolumeList"
            :key="stock.isuCd"
            class="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-50 transition-colors cursor-default"
          >
            <!-- 순위 뱃지 -->
            <span :class="rankBadgeClass(index)" class="w-7 h-7 flex-shrink-0 flex items-center justify-center rounded-full text-xs font-bold">
              {{ index + 1 }}
            </span>
            <!-- 종목 정보 -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-1.5">
                <span class="font-semibold text-gray-800 text-sm truncate">{{ stock.isuNm }}</span>
                <span :class="marketBadgeClass(stock.mktNm)" class="text-[10px] px-1.5 py-0.5 rounded font-medium flex-shrink-0">
                  {{ stock.mktNm }}
                </span>
              </div>
              <div class="text-xs text-gray-400 mt-0.5">{{ stock.isuSrtCd }}</div>
            </div>
            <!-- 가격 & 거래량 -->
            <div class="text-right flex-shrink-0">
              <div class="text-sm font-semibold text-gray-800">{{ formatPrice(stock.tddClsprc) }}</div>
              <div class="text-sm font-bold text-blue-600">{{ formatVolume(stock.accTrdvol) }}</div>
            </div>
          </li>
        </ul>
      </div>
    </div>

    <!-- 주요 이벤트 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
      <div class="bg-white rounded-xl shadow-sm p-6 border border-gray-200">
        <h2 class="text-xl font-bold text-gray-800 mb-4">오늘의 주요 뉴스</h2>
        <ul class="space-y-3">
          <!-- <li 
            v-for="(event, index) in marketEvents" 
            :key="index"
            class="flex items-start"
          >
            <span class="inline-block w-2 h-2 mt-2 mr-3 bg-blue-500 rounded-full"></span>
            <span class="text-gray-600">{{ event }}</span>
          </li> -->
          <li
            v-for="(news, index) in newsList"
            :key="index"
            class="flex items-start"
          >
            <span class="inline-block w-2 h-2 mt-2 mr-3 flex-shrink-0 bg-blue-500 rounded-full"></span>
            <a
              :href="news.link"
              target="_blank"
              rel="noopener noreferrer"
              class="text-gray-600 hover:text-blue-600 hover:underline text-sm leading-snug"
            >{{ news.title }}</a>
          </li>
        </ul>
      </div>

      <!-- 경제 캘린더 -->
      <div class="bg-white rounded-xl shadow-sm p-6 border border-gray-200">
        <h2 class="text-xl font-bold text-gray-800 mb-4">공모주 일정</h2>
          <!-- 캘린더 조건부 렌더링 -->
          <Calendar 
          v-if="calendarEvents && calendarEvents.length > 0" 
          :events="calendarEvents" 
        />
      
        <!-- 데이터가 없을 때 표시 -->
        <p v-else class="text-gray-600">현재 공모주 일정이 없습니다.</p>
        <!-- <ul class="space-y-3">
          <li 
            v-for="(event, index) in calendarEvents" 
            :key="index"
            class="flex items-start"
          >
            <span class="inline-block w-2 h-2 mt-2 mr-3 bg-blue-500 rounded-full"></span>
            <span class="text-gray-600">{{ event }}</span>
          </li>
        </ul> -->
      </div>
    </div>
  </div>
</template>

<style scoped>
:deep(.fc) {
  font-family: inherit;
}

:deep(.fc-toolbar-title) {
  font-size: 1.1em !important;
}

:deep(.fc-event) {
  background-color: #3b82f6;
  border-color: #3b82f6;
  padding: 2px;
}

:deep(.fc-day-today) {
  background-color: #eff6ff !important;
}
</style>