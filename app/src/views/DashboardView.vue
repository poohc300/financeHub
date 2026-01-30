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
    return ipoList.value.map(ipo => {
      // period 값을 '~'로 분리하여 시작일과 종료일로 분리
      const startDate = ipo.period.split('~')[0];
      const endDate = ipo.period.split('~')[1];
      const year = startDate.split('.')[0];

      // const startFormatted = startDate.replace(/\./g, '-');
      // const endFormatted = `${year}-${endDate.replace(/\./g, '-')}`; // '-' 로 변환환
      const startFormatted = `${startDate.replace(/\./g, '-')}T00:00:00`;
      const endFormatted = `${year}-${endDate.replace(/\./g, '-')}T23:59:59`;

      console.log(startFormatted);
      console.log(endFormatted);
      console.log(ipo.companyName);
      

      return {
        title: ipo.companyName,
        start: startFormatted,  // 변환된 시작일
        end: endFormatted,      // 변환된 종료일
      };
    }) || [];
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
  // 오일은 등락률이 제공되지 않아서 전날 오일값의 값과 금일값 비교해야함함
  return {
    label: data.oilNm,
    value: data.wtAvgPrc,
    change: '',
    isPositive: true
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
        <h2 class="text-xl font-bold text-gray-800 mb-4">오늘의 상승률 TOP 5</h2>
        <div class="overflow-x-auto">
          <table class="w-full">
            <thead>
              <tr class="border-b border-gray-200">
                <th class="text-left py-3 px-4 text-sm font-medium text-gray-500">순위</th>
                <th class="text-left py-3 px-4 text-sm font-medium text-gray-500">종목명</th>
                <th class="text-right py-3 px-4 text-sm font-medium text-gray-500">현재가</th>
                <th class="text-right py-3 px-4 text-sm font-medium text-gray-500">등락률</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(stock, index) in topGainersList"
                :key="stock.isuCd"
                class="border-b border-gray-100 hover:bg-gray-50"
              >
                <td class="py-3 px-4 text-sm">{{ index + 1 }}</td>
                <td class="py-3 px-4 font-medium">{{ stock.isuNm }}</td>
                <td class="py-3 px-4 text-right">{{ stock.tddClsprc }}</td>
                <td class="py-3 px-4 text-right text-green-600">{{ stock.flucRt }}%</td>
              </tr>
              <tr v-if="topGainersList.length === 0">
                <td colspan="4" class="py-3 px-4 text-center text-gray-500">데이터가 없습니다</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 거래량 TOP 5 -->
      <div class="bg-white rounded-xl shadow-sm p-6 border border-gray-200">
        <h2 class="text-xl font-bold text-gray-800 mb-4">오늘의 거래량 TOP 5</h2>
        <div class="overflow-x-auto">
          <table class="w-full">
            <thead>
              <tr class="border-b border-gray-200">
                <th class="text-left py-3 px-4 text-sm font-medium text-gray-500">순위</th>
                <th class="text-left py-3 px-4 text-sm font-medium text-gray-500">종목명</th>
                <th class="text-right py-3 px-4 text-sm font-medium text-gray-500">현재가</th>
                <th class="text-right py-3 px-4 text-sm font-medium text-gray-500">거래량</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(stock, index) in topVolumeList"
                :key="stock.isuCd"
                class="border-b border-gray-100 hover:bg-gray-50"
              >
                <td class="py-3 px-4 text-sm">{{ index + 1 }}</td>
                <td class="py-3 px-4 font-medium">{{ stock.isuNm }}</td>
                <td class="py-3 px-4 text-right">{{ stock.tddClsprc }}</td>
                <td class="py-3 px-4 text-right text-gray-600">{{ stock.accTrdvol }}</td>
              </tr>
              <tr v-if="topVolumeList.length === 0">
                <td colspan="4" class="py-3 px-4 text-center text-gray-500">데이터가 없습니다</td>
              </tr>
            </tbody>
          </table>
        </div>
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
            v-for="(event, index) in newsList" 
            :key="index"
            class="flex items-start"
          >
            <span class="inline-block w-2 h-2 mt-2 mr-3 bg-blue-500 rounded-full"></span>
            <span class="text-gray-600">{{ event }}</span>
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