<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue';
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
interface FuturesItem {
  isuCd: string
  isuNm: string
  currentPrice: string
  change: string
  changeRate: string
  open: string
  high: string
  low: string
}
const realtimeRanking = ref<RankingItem[]>([])
const realtimeFutures = ref<FuturesItem | null>(null)
const isMarketOpen = ref(false)
const marketReady = ref(false)  // WebSocket에서 첫 market 메시지 수신 후 true
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

// 핵심 5개 (디폴트 표시) — 데이터 없을 때 안내 문구 표시
const keyIndicators = computed<KeySlot[]>(() => {
  const kospi = kospiInfo.value.find(x => x.idxNm === '코스피');
  const kosdaq = kosdaqInfo.value.find(x => x.idxNm === '코스닥');
  const gold = goldMarketInfo.value.find(x => x.isuNm.includes('금 99.99_1kg'));
  const gasoline = oilMarketInfo.value.find(x => x.oilNm === '휘발유');
  const futures = realtimeFutures.value;
  return [
    { icon: '📈', label: '코스피',        indicator: kospi    ? toIndicator('📈', '코스피',        kospi.clsprcIdx,    kospi.flucRt)              : null },
    { icon: '📊', label: '코스닥',        indicator: kosdaq   ? toIndicator('📊', '코스닥',        kosdaq.clsprcIdx,   kosdaq.flucRt)             : null },
    { icon: '📉', label: 'K200 선물',     indicator: futures  ? toIndicator('📉', futures.isuNm,   futures.currentPrice, futures.changeRate)        : null },
    { icon: '🥇', label: '금 현물 1kg',  indicator: gold     ? toIndicator('🥇', '금 현물 1kg',  gold.tddClsprc,     gold.flucRt,     '원/g')   : null },
    { icon: '⛽', label: '휘발유',        indicator: gasoline ? toIndicator('⛽', '휘발유',        gasoline.wtAvgPrc,  gasoline.flucRt, '원/ℓ')  : null },
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

// 실시간 랭킹 필터 탭
type RankingFilter = 'all' | 'upper' | 'ipo'
const rankingFilter = ref<RankingFilter>('all')

const stockType = (rate: string): 'upper' | 'ipo' | 'normal' => {
  const r = parseFloat(rate)
  if (r >= 29.5 && r <= 30.5) return 'upper'
  if (r > 30.5) return 'ipo'
  return 'normal'
}

const PAGE_SIZE = 5
const rankingPage = ref(0)

const filteredRanking = computed(() => {
  if (rankingFilter.value === 'all') return realtimeRanking.value
  return realtimeRanking.value.filter(item => stockType(item.changeRate) === rankingFilter.value)
})

// 필터 변경 시 첫 페이지로
watch(rankingFilter, () => { rankingPage.value = 0 })

const pagedRanking = computed(() =>
  filteredRanking.value.slice(rankingPage.value * PAGE_SIZE, (rankingPage.value + 1) * PAGE_SIZE)
)

const totalPages = computed(() => Math.ceil(filteredRanking.value.length / PAGE_SIZE))

// 모바일 스와이프
let touchStartX = 0
const onTouchStart = (e: TouchEvent) => { touchStartX = e.touches[0].clientX }
const onTouchEnd = (e: TouchEvent) => {
  const dx = e.changedTouches[0].clientX - touchStartX
  if (dx < -50 && rankingPage.value < totalPages.value - 1) rankingPage.value++
  else if (dx > 50 && rankingPage.value > 0) rankingPage.value--
}

// 순위 뱃지 색상 (1위=금, 2위=은, 3위=동, 나머지=회색)
const rankBadgeClass = (pageIndex: number): string => {
  if (rankingPage.value === 0) {
    if (pageIndex === 0) return 'bg-yellow-400 text-white';
    if (pageIndex === 1) return 'bg-gray-300 text-gray-700';
    if (pageIndex === 2) return 'bg-orange-400 text-white';
  }
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
      if (msg.type === 'futures') {
        // KOSPI200 선물 근월물 실시간 갱신 (items 배열에서 첫 번째 = 근월물)
        if (msg.items?.length > 0) {
          realtimeFutures.value = msg.items[0]
          lastUpdated.value = new Date()
        }
      } else if (msg.type === 'market') {
        // KOSPI/KOSDAQ 현재지수 갱신 — ref.value 교체로 Vue 반응성 확실히 보장
        msg.items?.forEach((item: any) => {
          if (item.idxNm === '코스피' && kospiInfo.value.length > 0) {
            kospiInfo.value = kospiInfo.value.map(x =>
              x.idxNm === '코스피' ? { ...x, clsprcIdx: item.currentIdx, flucRt: item.changeRate } : x
            )
          } else if (item.idxNm === '코스닥' && kosdaqInfo.value.length > 0) {
            kosdaqInfo.value = kosdaqInfo.value.map(x =>
              x.idxNm === '코스닥' ? { ...x, clsprcIdx: item.currentIdx, flucRt: item.changeRate } : x
            )
          }
        })
        marketReady.value = true
        lastUpdated.value = new Date()
      } else if (msg.type === 'ranking') {
        realtimeRanking.value = msg.items || []
        isMarketOpen.value = true
        lastUpdated.value = new Date()
      } else if (!msg.type && msg.isuSrtCd) {
        // 개별 종목 실시간 체결가 (KisStockPrice) — ranking 아이템 가격 tick 갱신
        const idx = realtimeRanking.value.findIndex(r => r.isuSrtCd === msg.isuSrtCd)
        if (idx !== -1) {
          const updated = [...realtimeRanking.value]
          updated[idx] = { ...updated[idx], currentPrice: msg.currentPrice, change: msg.change, changeRate: msg.changeRate }
          realtimeRanking.value = updated
        }
      }
    } catch (e) {
      console.error('Dashboard WS 파싱 오류', e)
    }
  }
  ws.onerror = (e) => console.error('Dashboard WS 오류', e)
  // 장 마감 시 isMarketOpen만 false로 — realtimeRanking은 유지해서 당일 마지막 데이터 보존
  ws.onclose = () => { isMarketOpen.value = false }
}

// fetchDashboardData 완료 후 WS 연결 — 초기 캐시 메시지 수신 시 kospiInfo가 비어있는 race condition 방지
onMounted(async () => {
  await fetchDashboardData();
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

      <!-- 핵심 5개 카드 (2열 모바일 / 3열 md / 5열 lg) -->
      <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
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
              <!-- 선물(i===2)은 소수점 포맷, 나머지는 정수 천단위 포맷 -->
              <template v-if="i === 2">{{ parseFloat(slot.indicator.value).toFixed(2) }}</template>
              <template v-else>{{ Number(slot.indicator.value.replace(/,/g, '')).toLocaleString('ko-KR') }}</template>
              <span class="text-xs font-normal text-gray-400 ml-1">{{ slot.indicator.unit }}</span>
            </div>
            <!-- 코스피·코스닥(i<2)은 WebSocket 수신 전까지 등락률 숨김. 선물(i===2)은 futuresReady -->
            <div v-if="i >= 2 || marketReady"
              :class="slot.indicator.isPositive ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'"
              class="inline-flex items-center gap-1 text-sm font-semibold px-2 py-0.5 rounded-full"
            >
              <span>{{ slot.indicator.isPositive ? '▲' : '▼' }}</span>
              <span>{{ Math.abs(parseFloat(slot.indicator.change)).toFixed(2) }}%</span>
            </div>
            <div v-else class="inline-flex items-center gap-1 text-sm px-2 py-0.5 rounded-full bg-gray-100 text-gray-400">
              <span class="animate-pulse">···</span>
            </div>
          </template>
          <template v-else>
            <div class="text-2xl font-bold text-gray-200 mb-2">—</div>
            <div class="text-xs text-gray-400">
              {{ i === 2 ? '장 중 실시간 수신' : '데이터 없음 · 평일 16시 수집' }}
            </div>
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
      <!-- 상승률 랭킹 -->
      <div class="bg-white rounded-xl shadow-sm p-6 border border-gray-200">
        <div class="flex items-center gap-2 mb-5">
          <span class="text-xl">🚀</span>
          <h2 class="text-lg font-bold text-gray-800">실시간 상승률 랭킹</h2>
          <span v-if="isMarketOpen && realtimeRanking.length > 0"
            class="text-[10px] px-1.5 py-0.5 bg-green-100 text-green-700 rounded font-medium">LIVE</span>
          <span v-else-if="!isMarketOpen && realtimeRanking.length > 0"
            class="text-[10px] px-1.5 py-0.5 bg-gray-100 text-gray-500 rounded font-medium">장 마감</span>
        </div>

        <!-- 필터 탭 -->
        <div v-if="realtimeRanking.length > 0" class="flex gap-1 mb-3">
          <button
            v-for="tab in ([{ key: 'all', label: '전체' }, { key: 'upper', label: '상한가' }, { key: 'ipo', label: '신규상장' }] as const)"
            :key="tab.key"
            @click="rankingFilter = tab.key"
            :class="rankingFilter === tab.key
              ? tab.key === 'upper' ? 'bg-red-500 text-white' : tab.key === 'ipo' ? 'bg-purple-500 text-white' : 'bg-gray-700 text-white'
              : 'bg-gray-100 text-gray-500 hover:bg-gray-200'"
            class="text-xs px-2.5 py-1 rounded-full font-medium transition-colors"
          >{{ tab.label }}</button>
        </div>

        <!-- 랭킹 (장 중: LIVE / 장 마감: 당일 마지막 데이터) -->
        <template v-if="realtimeRanking.length > 0">
          <div
            @touchstart.passive="onTouchStart"
            @touchend.passive="onTouchEnd"
            class="select-none"
          >
          <div v-if="filteredRanking.length === 0" class="flex flex-col items-center justify-center py-8 text-gray-400">
            <span class="text-2xl mb-1">—</span>
            <span class="text-sm">해당 종목 없음</span>
          </div>
          <ul v-else class="space-y-2">
          <li
            v-for="(item, index) in pagedRanking"
            :key="item.isuSrtCd"
            class="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-50 transition-colors cursor-default"
          >
            <span :class="rankBadgeClass(index)" class="w-7 h-7 flex-shrink-0 flex items-center justify-center rounded-full text-xs font-bold">
              {{ rankingPage * PAGE_SIZE + index + 1 }}
            </span>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-1.5">
                <span class="font-semibold text-gray-800 text-sm truncate">{{ item.isuNm }}</span>
                <span v-if="parseFloat(item.changeRate) >= 29.5 && parseFloat(item.changeRate) <= 30.5"
                  class="flex-shrink-0 text-[10px] px-1.5 py-0.5 bg-red-500 text-white rounded font-bold tracking-tight">
                  상한가
                </span>
                <span v-else-if="parseFloat(item.changeRate) > 30.5"
                  class="flex-shrink-0 text-[10px] px-1.5 py-0.5 bg-purple-500 text-white rounded font-bold tracking-tight">
                  신규상장
                </span>
              </div>
              <div class="text-xs text-gray-400 mt-0.5">{{ item.isuSrtCd }}</div>
            </div>
            <div class="text-right flex-shrink-0">
              <div class="text-sm font-semibold text-gray-800">{{ Number(item.currentPrice).toLocaleString('ko-KR') }}원</div>
              <div class="flex items-center justify-end gap-0.5" :class="parseFloat(item.changeRate) >= 29.5 && parseFloat(item.changeRate) <= 30.5 ? 'text-red-500' : parseFloat(item.changeRate) > 30.5 ? 'text-purple-600' : parseFloat(item.changeRate) >= 0 ? 'text-green-600' : 'text-red-600'">
                <span class="text-xs">{{ parseFloat(item.changeRate) >= 0 ? '▲' : '▼' }}</span>
                <span class="text-sm font-bold">{{ Math.abs(parseFloat(item.changeRate)).toFixed(2) }}%</span>
              </div>
            </div>
          </li>
        </ul>

        <!-- 페이지네이션 -->
        <div v-if="totalPages > 1" class="flex items-center justify-center gap-2 mt-3">
          <button
            @click="rankingPage--"
            :disabled="rankingPage === 0"
            class="w-7 h-7 flex items-center justify-center rounded-full text-sm text-gray-500 hover:bg-gray-100 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
          >‹</button>
          <span class="text-xs text-gray-400 tabular-nums">{{ rankingPage + 1 }} / {{ totalPages }}</span>
          <button
            @click="rankingPage++"
            :disabled="rankingPage >= totalPages - 1"
            class="w-7 h-7 flex items-center justify-center rounded-full text-sm text-gray-500 hover:bg-gray-100 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
          >›</button>
        </div>
          </div><!-- swipe wrapper -->
        </template>

        <!-- 장 시작 전 / 페이지 로드 시 실시간 데이터 없는 경우 -->
        <template v-else>
          <div class="flex flex-col items-center justify-center py-10 text-gray-400">
            <span class="text-3xl mb-2">📈</span>
            <span class="text-sm font-medium">장 시작 후 실시간 데이터가 표시됩니다</span>
            <span class="text-xs mt-1">평일 09:00 ~ 15:30</span>
          </div>
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