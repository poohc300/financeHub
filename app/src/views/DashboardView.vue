<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { openLink } from '../util/openLink';
import { CrawledIpoDTO, CrawledNewsDTO, GoldMarketTradingDTO, DashboardDataDTO, OilMarketDailtyTradingDTO, KospiDailyTradingDTO, KosdaqDailyTradingDTO, StockDailyTradingDTO } from '../model/DashboardDataDTO';
import { fetchRequest } from '../util/fetchRequest';
import Calendar from '../components/Calendar.vue';
import { CalendarEventDTO } from '../model/CalendarEventDTO';

const ipoList = ref<CrawledIpoDTO[]>([]);
const newsList = ref<CrawledNewsDTO[]>([]);
const goldMarketInfo = ref<GoldMarketTradingDTO[]>([]);
const oilMarketInfo = ref<OilMarketDailtyTradingDTO[]>([]);
const kospiInfo = ref<KospiDailyTradingDTO[]>([]);
const kosdaqInfo = ref<KosdaqDailyTradingDTO[]>([]);
const topGainersList = ref<StockDailyTradingDTO[]>([]);
const topVolumeList = ref<StockDailyTradingDTO[]>([]);
const showAllIndicators = ref(false);
const lastUpdated = ref<Date | null>(null);
const isRefreshing = ref(false);

// 실시간 (WebSocket)
interface RankingItem {
  isuSrtCd: string
  isuNm: string
  currentPrice: string
  change: string
  changeRate: string
  volume: string
}
const realtimeRanking = ref<RankingItem[]>([])
const isMarketOpen = ref(false)
let ws: WebSocket | null = null

const lastUpdatedText = computed(() => {
  if (!lastUpdated.value) return '';
  return lastUpdated.value.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
});

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

interface Indicator {
  icon: string;
  label: string;
  value: string;
  change: string;
  isPositive: boolean;
  unit: string;
}

const toIndicator = (icon: string, label: string, value: string, change: string, unit = ''): Indicator => ({
  icon, label, value,
  change: change || '0',
  isPositive: parseFloat(change || '0') >= 0,
  unit,
})

// fetch
const fetchDashboardData = async () => {
  try {
    const data = await fetchRequest<DashboardDataDTO>("/dashboard/data", "GET");
    newsList.value = data.crawledNewsList || [];
    ipoList.value = data.crawledIpoList || [];
    goldMarketInfo.value = data.goldMarketDailyTradingList || [];
    oilMarketInfo.value = data.oilMarketDailyTradingList || [];
    kospiInfo.value = data.kospiDailyTradingList || [];
    kosdaqInfo.value = data.kosdaqDailyTradingList || [];
    topGainersList.value = data.topGainersList || [];
    topVolumeList.value = data.topVolumeList || [];
    lastUpdated.value = new Date();
  } catch (error) {
    console.error("Error fetching dashboard data:", error);
    throw error;
  }
}

const refreshData = async () => {
  isRefreshing.value = true;
  try {
    await fetchDashboardData();
  } finally {
    isRefreshing.value = false;
  }
}

interface KeySlot { icon: string; label: string; indicator: Indicator | null }

// 핵심 4개 (디폴트 표시) — 데이터 없을 때 안내 문구 표시
const keyIndicators = computed<KeySlot[]>(() => {
  const kospi = kospiInfo.value.find(x => x.idxNm === '코스피');
  const kosdaq = kosdaqInfo.value.find(x => x.idxNm === '코스닥');
  const gold = goldMarketInfo.value.find(x => x.isuNm.includes('금 99.99_1kg'));
  const gasoline = oilMarketInfo.value.find(x => x.oilNm === '휘발유');
  return [
    { icon: '📈', label: '코스피',      indicator: kospi    ? toIndicator('📈', '코스피',      kospi.clsprcIdx,    kospi.flucRt)              : null },
    { icon: '📊', label: '코스닥',      indicator: kosdaq   ? toIndicator('📊', '코스닥',      kosdaq.clsprcIdx,   kosdaq.flucRt)             : null },
    { icon: '🥇', label: '금 현물 1kg', indicator: gold     ? toIndicator('🥇', '금 현물 1kg', gold.tddClsprc,     gold.flucRt,     '원/g')   : null },
    { icon: '⛽', label: '휘발유',      indicator: gasoline ? toIndicator('⛽', '휘발유',      gasoline.wtAvgPrc,  gasoline.flucRt, '원/ℓ')  : null },
  ];
})

// 전체 지표 그룹 (펼치기 시 표시)
const allIndicatorGroups = computed(() => [
  {
    title: 'KOSPI 업종별',
    items: kospiInfo.value.filter(x => x.idxNm !== '코스피')
      .map(x => toIndicator('', x.idxNm, x.clsprcIdx, x.flucRt)),
  },
  {
    title: 'KOSDAQ 업종별',
    items: kosdaqInfo.value.filter(x => x.idxNm !== '코스닥')
      .map(x => toIndicator('', x.idxNm, x.clsprcIdx, x.flucRt)),
  },
  {
    title: '원자재',
    items: goldMarketInfo.value.filter(x => !x.isuNm.includes('금 99.99_1kg'))
      .map(x => toIndicator('🥇', x.isuNm, x.tddClsprc, x.flucRt, '원/g')),
  },
  {
    title: '에너지',
    items: oilMarketInfo.value.filter(x => x.oilNm !== '휘발유')
      .map(x => toIndicator('⛽', x.oilNm, x.wtAvgPrc, x.flucRt, '원/ℓ')),
  },
].filter(g => g.items.length > 0))

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

const connectWebSocket = () => {
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  ws = new WebSocket(`${protocol}//${location.host}/ws/stock`)

  ws.onmessage = (event) => {
    try {
      const msg = JSON.parse(event.data)
      if (msg.type === 'market') {
        // KOSPI/KOSDAQ 현재지수 갱신
        msg.items?.forEach((item: any) => {
          if (item.idxNm === '코스피') {
            const entry = kospiInfo.value.find(x => x.idxNm === '코스피')
            if (entry) { entry.clsprcIdx = item.currentIdx; entry.flucRt = item.changeRate }
          } else if (item.idxNm === '코스닥') {
            const entry = kosdaqInfo.value.find(x => x.idxNm === '코스닥')
            if (entry) { entry.clsprcIdx = item.currentIdx; entry.flucRt = item.changeRate }
          }
        })
        lastUpdated.value = new Date()
      } else if (msg.type === 'ranking') {
        realtimeRanking.value = msg.items || []
        isMarketOpen.value = true
        lastUpdated.value = new Date()
      }
    } catch (e) {
      console.error('Dashboard WS 파싱 오류', e)
    }
  }
  ws.onerror = (e) => console.error('Dashboard WS 오류', e)
  ws.onclose = () => { isMarketOpen.value = false }
}

onMounted(() => {
  fetchDashboardData();
  connectWebSocket();
})

onUnmounted(() => {
  ws?.close()
  ws = null
})

</script>

<template>
  <div class="max-w-6xl mx-auto">

    <!-- 마지막 업데이트 시각 + 새로고침 -->
    <div class="flex justify-end items-center gap-3 mb-4 text-sm text-gray-400">
      <span v-if="lastUpdatedText">마지막 업데이트: {{ lastUpdatedText }}</span>
      <button
        @click="refreshData"
        :disabled="isRefreshing"
        class="flex items-center gap-1 px-3 py-1 rounded-lg border border-gray-200 hover:bg-gray-50 text-gray-500 hover:text-gray-700 transition-colors disabled:opacity-50"
      >
        <span :class="{ 'animate-spin': isRefreshing }" class="inline-block">↻</span>
        새로고침
      </button>
    </div>

    <!-- 주요 경제 지표 -->
    <div class="mb-8">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-xl font-bold text-gray-800">주요 경제 지표</h2>
        <button
          @click="showAllIndicators = !showAllIndicators"
          class="text-sm text-blue-500 hover:text-blue-700 font-medium transition-colors"
        >
          {{ showAllIndicators ? '접기 ▲' : '전체 지표 보기 ▼' }}
        </button>
      </div>

      <!-- 핵심 4개 카드 -->
      <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div
          v-for="(slot, i) in keyIndicators"
          :key="i"
          class="bg-white rounded-xl border border-gray-200 p-5 hover:shadow-md transition-shadow"
        >
          <div class="flex items-center gap-2 mb-3">
            <span class="text-xl">{{ slot.icon }}</span>
            <span class="text-sm font-medium text-gray-500">{{ slot.label }}</span>
          </div>
          <template v-if="slot.indicator">
            <div class="text-2xl font-bold text-gray-900 mb-2 tabular-nums">
              {{ Number(slot.indicator.value.replace(/,/g, '')).toLocaleString('ko-KR') }}
              <span class="text-xs font-normal text-gray-400 ml-1">{{ slot.indicator.unit }}</span>
            </div>
            <div
              :class="slot.indicator.isPositive ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'"
              class="inline-flex items-center gap-1 text-sm font-semibold px-2 py-0.5 rounded-full"
            >
              <span>{{ slot.indicator.isPositive ? '▲' : '▼' }}</span>
              <span>{{ Math.abs(parseFloat(slot.indicator.change)).toFixed(2) }}%</span>
            </div>
          </template>
          <template v-else>
            <div class="text-2xl font-bold text-gray-200 mb-2">—</div>
            <div class="text-xs text-gray-400">데이터 없음 · 평일 16시 수집</div>
          </template>
        </div>
      </div>

      <!-- 전체 지표 펼치기 -->
      <div v-if="showAllIndicators" class="mt-4 space-y-5">
        <div v-for="group in allIndicatorGroups" :key="group.title">
          <h3 class="text-sm font-semibold text-gray-500 mb-2 pl-1">{{ group.title }}</h3>
          <div class="grid grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-2">
            <div
              v-for="(ind, j) in group.items"
              :key="j"
              class="bg-white rounded-lg border border-gray-100 p-3 hover:shadow-sm transition-shadow"
            >
              <div class="text-xs text-gray-500 mb-1 truncate" :title="ind.label">{{ ind.label }}</div>
              <div class="text-sm font-bold text-gray-800 tabular-nums">
                {{ Number(ind.value.replace(/,/g, '')).toLocaleString('ko-KR') }}
              </div>
              <div
                :class="ind.isPositive ? 'text-green-600' : 'text-red-600'"
                class="text-xs font-medium mt-0.5"
              >
                {{ ind.isPositive ? '▲' : '▼' }} {{ Math.abs(parseFloat(ind.change)).toFixed(2) }}%
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 상승률 TOP 5 & 거래량 TOP 5 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
      <!-- 상승률 TOP 5 -->
      <div class="bg-white rounded-xl shadow-sm p-6 border border-gray-200">
        <div class="flex items-center gap-2 mb-5">
          <span class="text-xl">🚀</span>
          <h2 class="text-lg font-bold text-gray-800">
            {{ isMarketOpen && realtimeRanking.length > 0 ? '실시간 상승률 TOP 5' : '오늘의 상승률 TOP 5' }}
          </h2>
          <span v-if="isMarketOpen && realtimeRanking.length > 0" class="text-[10px] px-1.5 py-0.5 bg-green-100 text-green-700 rounded font-medium">LIVE</span>
        </div>

        <!-- 실시간 랭킹 (장 중) -->
        <ul v-if="isMarketOpen && realtimeRanking.length > 0" class="space-y-2">
          <li
            v-for="(item, index) in realtimeRanking"
            :key="item.isuSrtCd"
            class="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-50 transition-colors cursor-default"
          >
            <span :class="rankBadgeClass(index)" class="w-7 h-7 flex-shrink-0 flex items-center justify-center rounded-full text-xs font-bold">
              {{ index + 1 }}
            </span>
            <div class="flex-1 min-w-0">
              <div class="font-semibold text-gray-800 text-sm truncate">{{ item.isuNm }}</div>
              <div class="text-xs text-gray-400 mt-0.5">{{ item.isuSrtCd }}</div>
            </div>
            <div class="text-right flex-shrink-0">
              <div class="text-sm font-semibold text-gray-800">{{ Number(item.currentPrice).toLocaleString('ko-KR') }}원</div>
              <div class="flex items-center justify-end gap-0.5" :class="parseFloat(item.changeRate) >= 0 ? 'text-green-600' : 'text-red-600'">
                <span class="text-xs">{{ parseFloat(item.changeRate) >= 0 ? '▲' : '▼' }}</span>
                <span class="text-sm font-bold">{{ Math.abs(parseFloat(item.changeRate)).toFixed(2) }}%</span>
              </div>
            </div>
          </li>
        </ul>

        <!-- KRX 정적 랭킹 (장 마감 후) -->
        <template v-else>
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
              <span :class="rankBadgeClass(index)" class="w-7 h-7 flex-shrink-0 flex items-center justify-center rounded-full text-xs font-bold">
                {{ index + 1 }}
              </span>
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-1.5">
                  <span class="font-semibold text-gray-800 text-sm truncate">{{ stock.isuNm }}</span>
                  <span :class="marketBadgeClass(stock.mktNm)" class="text-[10px] px-1.5 py-0.5 rounded font-medium flex-shrink-0">
                    {{ stock.mktNm }}
                  </span>
                </div>
                <div class="text-xs text-gray-400 mt-0.5">{{ stock.isuSrtCd }}</div>
              </div>
              <div class="text-right flex-shrink-0">
                <div class="text-sm font-semibold text-gray-800">{{ formatPrice(stock.tddClsprc) }}</div>
                <div class="flex items-center justify-end gap-0.5 text-green-600">
                  <span class="text-xs">▲</span>
                  <span class="text-sm font-bold">{{ stock.flucRt }}%</span>
                </div>
              </div>
            </li>
          </ul>
        </template>
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
              href="javascript:void(0)"
              @click="openLink(news.link)"
              class="text-gray-600 hover:text-blue-600 hover:underline text-sm leading-snug cursor-pointer"
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