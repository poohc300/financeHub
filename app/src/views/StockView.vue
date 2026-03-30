<script setup lang="ts">
import { Line, Bar } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale, LinearScale, TimeScale, TimeSeriesScale, PointElement, LineElement, BarElement,
  Title, Tooltip, Legend, Chart
} from 'chart.js'
import {
  CandlestickController, CandlestickElement
} from 'chartjs-chart-financial'
import 'chartjs-adapter-date-fns'
import { ref, onMounted, computed, watch, onBeforeUnmount, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { fetchRequest } from '../util/fetchRequest'
import { StockDailyTradingDTO } from '../model/DashboardDataDTO'

const route = useRoute()

// 관심 종목 (localStorage)
const WATCHLIST_KEY = 'watchlist'
const MAX_WATCHLIST = 5
const isInWatchlist = ref(false)

const loadWatchlistState = () => {
  try {
    const list = JSON.parse(localStorage.getItem(WATCHLIST_KEY) || '[]')
    isInWatchlist.value = list.some((w: any) => w.isuSrtCd === selectedIsuSrtCd.value)
  } catch {
    isInWatchlist.value = false
  }
}

const toggleWatchlist = () => {
  try {
    const list: any[] = JSON.parse(localStorage.getItem(WATCHLIST_KEY) || '[]')
    if (isInWatchlist.value) {
      const updated = list.filter((w: any) => w.isuSrtCd !== selectedIsuSrtCd.value)
      localStorage.setItem(WATCHLIST_KEY, JSON.stringify(updated))
      isInWatchlist.value = false
    } else {
      if (list.length >= MAX_WATCHLIST) {
        alert(`관심 종목은 최대 ${MAX_WATCHLIST}개까지 등록할 수 있습니다.`)
        return
      }
      list.push({ isuSrtCd: selectedIsuSrtCd.value, isuCd: selectedIsuCd.value, isuNm: selectedIndex.value })
      localStorage.setItem(WATCHLIST_KEY, JSON.stringify(list))
      isInWatchlist.value = true
    }
  } catch {}
}

ChartJS.register(
  CategoryScale, LinearScale, TimeScale, TimeSeriesScale, PointElement, LineElement, BarElement,
  Title, Tooltip, Legend,
  CandlestickController, CandlestickElement
)

interface ChartDataDTO {
  labels: string[]
  values: string[]
  volumes: string[]
  opens: string[]
  highs: string[]
  lows: string[]
  indexName: string
}

const formatDate = (d: Date) => {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const todayDate = new Date()
const weekAgoDate = new Date(todayDate)
weekAgoDate.setDate(todayDate.getDate() - 7)

const selectedMarket = ref('KOSPI')
const selectedIndex = ref('코스피')
const selectedPeriod = ref(0)
const startDate = ref(formatDate(weekAgoDate))
const endDate = ref(formatDate(todayDate))
const chartType = ref<'line' | 'candle'>('line')
const compareMode = ref(false)

const searchQuery = ref('')
const searchResults = ref<StockDailyTradingDTO[]>([])
const showDropdown = ref(false)
const selectedIsuCd = ref('')
const selectedIsuSrtCd = ref('')
let searchTimer: ReturnType<typeof setTimeout> | null = null

const chartLabels = ref<string[]>([])
const chartValues = ref<string[]>([])
const chartVolumes = ref<string[]>([])
const chartOpens = ref<string[]>([])
const chartHighs = ref<string[]>([])
const chartLows = ref<string[]>([])
const compareValues = ref<string[]>([])
const topVolumeList = ref<StockDailyTradingDTO[]>([])

// 실시간 가격 캐시 (isuSrtCd → 가격)
const realtimePrices = ref<Record<string, {
  isuSrtCd: string
  currentPrice: string
  change: string
  changeRate: string
  volume: string
  time: string
  open: string
  high: string
  low: string
}>>({})
// 선택된 종목의 실시간 가격
const selectedRealtimePrice = ref<typeof realtimePrices.value[string] | null>(null)
// 선택된 종목의 DB 마지막 종가 (실시간 없을 때 fallback)
const selectedStaticPrice = ref<{ currentPrice: string; change: string; changeRate: string } | null>(null)
// 실시간 TOP 5 랭킹 (ranking 메시지 수신 시 갱신)
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
let wsReconnectTimer: ReturnType<typeof setTimeout> | null = null
let wsIntentionalClose = false

const candleCanvasRef = ref<HTMLCanvasElement | null>(null)
let candleChartInstance: Chart | null = null

const periodOptions = [
  { label: '1주', value: 7 },
  { label: '1개월', value: 30 },
  { label: '3개월', value: 90 },
  { label: '1년', value: 365 }
]

const marketButtons = [
  { label: 'KOSPI', market: 'KOSPI', index: '코스피' },
  { label: 'KOSDAQ', market: 'KOSDAQ', index: '코스닥' },
]

const marketColor = computed(() => {
  const map: Record<string, string> = {
    KOSPI: '59, 130, 246',
    KOSDAQ: '234, 88, 12',
    STOCK: '16, 185, 129',
  }
  return map[selectedMarket.value] || '59, 130, 246'
})

const formattedLabels = computed(() =>
  chartLabels.value.map(label =>
    label?.length === 8 ? `${label.substring(4, 6)}/${label.substring(6, 8)}` : label
  )
)

const parseNum = (v: string) => parseFloat(v?.replace(/,/g, '') || '0')

const hasOhlc = computed(() =>
  chartOpens.value.length > 0 && parseNum(chartOpens.value[0]) > 0
)

const lineChartData = computed(() => {
  const datasets: any[] = [{
    label: selectedIndex.value,
    data: chartValues.value.map(parseNum),
    borderColor: `rgb(${marketColor.value})`,
    backgroundColor: `rgba(${marketColor.value}, 0.1)`,
    tension: 0.4,
    fill: true,
  }]
  if (compareMode.value && compareValues.value.length > 0) {
    datasets.push({
      label: '코스닥',
      data: compareValues.value.map(parseNum),
      borderColor: 'rgb(234, 88, 12)',
      backgroundColor: 'rgba(234, 88, 12, 0.05)',
      tension: 0.4,
      fill: false,
      yAxisID: 'y2',
    })
  }
  return { labels: formattedLabels.value, datasets }
})

const lineChartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    title: {
      display: true,
      text: compareMode.value ? 'KOSPI vs 코스닥 비교' : `${selectedIndex.value} 추이`,
      font: { size: 16, weight: 'bold' as const }
    }
  },
  scales: {
    y: { beginAtZero: false, grid: { color: 'rgba(0,0,0,0.1)' } },
    ...(compareMode.value ? {
      y2: { beginAtZero: false, position: 'right' as const, grid: { drawOnChartArea: false } }
    } : {}),
    x: { grid: { display: false } }
  }
}))

const volumeChartData = computed(() => ({
  labels: formattedLabels.value,
  datasets: [{
    label: '거래량',
    data: chartVolumes.value.map(parseNum),
    backgroundColor: `rgba(${marketColor.value}, 0.6)`,
    borderColor: `rgb(${marketColor.value})`,
    borderWidth: 1
  }]
}))

const volumeChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    title: { display: true, text: '거래량', font: { size: 14, weight: 'bold' as const } }
  },
  scales: {
    y: { beginAtZero: true, grid: { color: 'rgba(0,0,0,0.1)' } },
    x: { grid: { display: false } }
  }
}

const renderCandleChart = () => {
  if (!candleCanvasRef.value) return
  if (candleChartInstance) {
    candleChartInstance.destroy()
    candleChartInstance = null
  }
  candleChartInstance = new Chart(candleCanvasRef.value, {
    type: 'candlestick' as any,
    data: {
      datasets: [{
        label: selectedIndex.value,
        data: chartLabels.value.map((label, i) => ({
          x: label?.length === 8
            ? new Date(`${label.substring(0,4)}-${label.substring(4,6)}-${label.substring(6,8)}`).getTime()
            : new Date(label).getTime(),
          o: parseNum(chartOpens.value[i]),
          h: parseNum(chartHighs.value[i]),
          l: parseNum(chartLows.value[i]),
          c: parseNum(chartValues.value[i]),
        })),
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: 'top' },
        title: { display: true, text: `${selectedIndex.value} 캔들차트`, font: { size: 16, weight: 'bold' } }
      },
      scales: {
        x: {
          type: 'timeseries' as any,
          grid: { display: false },
          time: {
            unit: selectedPeriod.value <= 30 ? 'day' : selectedPeriod.value <= 90 ? 'week' : 'month'
          }
        },
        y: { beginAtZero: false }
      }
    }
  })
}

watch(chartType, () => {
  if (chartType.value === 'candle') {
    requestAnimationFrame(renderCandleChart)
  } else {
    candleChartInstance?.destroy()
    candleChartInstance = null
  }
})

watch(chartLabels, () => {
  if (chartType.value === 'candle') {
    requestAnimationFrame(renderCandleChart)
  }
})

onBeforeUnmount(() => {
  candleChartInstance?.destroy()
  wsIntentionalClose = true
  if (wsReconnectTimer) clearTimeout(wsReconnectTimer)
  ws?.close()
})

const buildUrl = (market: string, index: string) => {
  let url: string
  if (market === 'STOCK') {
    url = `/dashboard/chart-data?market=STOCK&isuCd=${encodeURIComponent(selectedIsuCd.value)}`
  } else {
    url = `/dashboard/chart-data?market=${market}&indexName=${encodeURIComponent(index)}`
  }
  if (startDate.value && endDate.value) {
    url += `&startDate=${startDate.value.replace(/-/g, '')}&endDate=${endDate.value.replace(/-/g, '')}`
  } else {
    url += `&limit=${selectedPeriod.value}`
  }
  return url
}

const onSearchInput = () => {
  if (searchTimer) clearTimeout(searchTimer)
  if (!searchQuery.value.trim()) {
    searchResults.value = []
    showDropdown.value = false
    return
  }
  searchTimer = setTimeout(async () => {
    try {
      const results = await fetchRequest<StockDailyTradingDTO[]>(
        `/dashboard/stocks/search?keyword=${encodeURIComponent(searchQuery.value)}&limit=10`, 'GET'
      )
      searchResults.value = results || []
      showDropdown.value = searchResults.value.length > 0
    } catch (e) {
      console.error('종목 검색 오류:', e)
    }
  }, 300)
}

const selectStock = (stock: StockDailyTradingDTO) => {
  selectedIsuCd.value = stock.isuCd
  selectedIsuSrtCd.value = stock.isuSrtCd
  searchQuery.value = stock.isuNm
  showDropdown.value = false
  selectedRealtimePrice.value = realtimePrices.value[stock.isuSrtCd] ?? null
  changeMarket('STOCK', stock.isuNm)
  loadWatchlistState()
}

const connectRealtime = () => {
  if (ws) ws.close()
  wsIntentionalClose = false
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  ws = new WebSocket(`${protocol}//${location.host}/ws/stock`)

  ws.onmessage = (event) => {
    try {
      const msg = JSON.parse(event.data)
      if (msg.type === 'ranking') {
        realtimeRanking.value = msg.items || []
        isMarketOpen.value = true
      } else if (msg.isuSrtCd) {
        realtimePrices.value = { ...realtimePrices.value, [msg.isuSrtCd]: msg }
        if (selectedIsuSrtCd.value === msg.isuSrtCd) {
          selectedRealtimePrice.value = msg
        }
        const idx = realtimeRanking.value.findIndex(r => r.isuSrtCd === msg.isuSrtCd)
        if (idx !== -1) {
          realtimeRanking.value = realtimeRanking.value.map((r, i) =>
            i === idx ? { ...r, currentPrice: msg.currentPrice, change: msg.change, changeRate: msg.changeRate, volume: msg.volume } : r
          )
        }
      }
    } catch (e) {
      console.error('WS 파싱 오류', e)
    }
  }
  ws.onerror = () => { /* onclose에서 재연결 처리 */ }
  ws.onclose = () => {
    if (wsIntentionalClose) return
    if (wsReconnectTimer) clearTimeout(wsReconnectTimer)
    wsReconnectTimer = setTimeout(connectRealtime, 5000)
  }
}

const closeDropdown = () => {
  setTimeout(() => { showDropdown.value = false }, 150)
}

const fetchChartData = async () => {
  try {
    const data = await fetchRequest<ChartDataDTO>(buildUrl(selectedMarket.value, selectedIndex.value), 'GET')
    chartLabels.value = data.labels || []
    chartValues.value = data.values || []
    chartVolumes.value = data.volumes || []
    chartOpens.value = data.opens || []
    chartHighs.value = data.highs || []
    chartLows.value = data.lows || []
    if (compareMode.value) await fetchCompareData()

    if (chartLabels.value.length === 0) {
      alert('선택한 기간의 데이터가 없습니다.\n더 짧은 기간을 선택하거나 데이터 수집 후 다시 시도하세요.')
    } else if (chartType.value === 'candle') {
      requestAnimationFrame(renderCandleChart)
    }

    // STOCK 선택 시 마지막 종가로 정적 가격 카드 구성 (실시간 없을 때 fallback)
    if (selectedMarket.value === 'STOCK' && chartValues.value.length >= 1) {
      const parse = (v: string) => parseFloat(v?.replace(/,/g, '') || '0')
      const vals = chartValues.value
      const curr = parse(vals[vals.length - 1])
      const prev = vals.length >= 2 ? parse(vals[vals.length - 2]) : curr
      const diff = curr - prev
      const rate = prev !== 0 ? (diff / prev) * 100 : 0
      selectedStaticPrice.value = {
        currentPrice: curr.toLocaleString(),
        change: (diff >= 0 ? '+' : '') + diff.toFixed(0),
        changeRate: (rate >= 0 ? '+' : '') + rate.toFixed(2),
      }
    } else {
      selectedStaticPrice.value = null
    }
  } catch (e) {
    console.error('차트 데이터 오류:', e)
  }
}

const fetchCompareData = async () => {
  try {
    const data = await fetchRequest<ChartDataDTO>(buildUrl('KOSDAQ', '코스닥'), 'GET')
    compareValues.value = data.values || []
  } catch (e) {
    console.error('비교 데이터 오류:', e)
  }
}

const fetchTopVolume = async () => {
  try {
    const data = await fetchRequest<{ topVolumeList: StockDailyTradingDTO[] }>('/dashboard/data', 'GET')
    topVolumeList.value = data.topVolumeList || []
  } catch (e) {
    console.error('거래량 TOP 오류:', e)
  }
}

const changeMarket = (market: string, indexName: string) => {
  selectedMarket.value = market
  selectedIndex.value = indexName
  compareMode.value = false
  fetchChartData()
}

const changePeriod = (period: number) => {
  const now = new Date()
  const from = new Date(now)
  from.setDate(now.getDate() - period)
  startDate.value = formatDate(from)
  endDate.value = formatDate(now)
  selectedPeriod.value = period
  fetchChartData()
}

const applyDateRange = () => {
  if (startDate.value && endDate.value) {
    selectedPeriod.value = 0
    fetchChartData()
  }
}

const clearDateRange = () => {
  const now = new Date()
  const weekAgo = new Date(now)
  weekAgo.setDate(now.getDate() - 7)
  startDate.value = formatDate(weekAgo)
  endDate.value = formatDate(now)
  selectedPeriod.value = 0
  fetchChartData()
}

const toggleCompare = () => {
  compareMode.value = !compareMode.value
  if (compareMode.value) {
    chartType.value = 'line'
    selectedMarket.value = 'KOSPI'
    selectedIndex.value = '코스피'
  }
  fetchChartData()
}

onMounted(() => {
  // 관심 종목 탭에서 넘어온 경우 바로 종목 선택
  if (route.query.isuCd && route.query.isuNm) {
    selectedIsuCd.value = route.query.isuCd as string
    selectedIsuSrtCd.value = (route.query.isuSrtCd as string) || ''
    searchQuery.value = route.query.isuNm as string
    selectedMarket.value = 'STOCK'
    selectedIndex.value = route.query.isuNm as string
    fetchChartData()
    loadWatchlistState()
  } else {
    fetchChartData()
  }
  fetchTopVolume()
  connectRealtime()
})
</script>

<template>
  <div class="max-w-6xl mx-auto">
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
      <!-- 사이드 패널 -->
      <div class="lg:col-span-1">
        <!-- 실시간 TOP 5 / 거래량 TOP -->
        <div>
          <div class="flex items-center gap-2 mb-4">
            <h2 class="text-xl font-bold text-gray-800">
              {{ isMarketOpen ? '실시간 상승률 TOP 5' : '거래량 TOP 5' }}
            </h2>
            <span v-if="isMarketOpen" class="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded-full font-medium">LIVE</span>
            <span v-else class="text-xs bg-gray-100 text-gray-500 px-2 py-0.5 rounded-full">전일 종가</span>
          </div>
          <div class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
            <div class="divide-y divide-gray-200">
              <!-- 장 중: 실시간 랭킹 상위 5개 -->
              <template v-if="isMarketOpen && realtimeRanking.length > 0">
                <div
                  v-for="(stock, index) in realtimeRanking.slice(0, 5)"
                  :key="stock.isuSrtCd"
                  class="p-3 hover:bg-gray-50 transition-colors duration-200 cursor-pointer"
                  @click="selectStock({ isuCd: stock.isuSrtCd, isuSrtCd: stock.isuSrtCd, isuNm: stock.isuNm, accTrdvol: stock.volume, tddClsprc: stock.currentPrice, flucRt: stock.changeRate } as any)"
                >
                  <div class="flex items-center gap-2">
                    <span class="text-xs font-bold w-5 text-center" :class="index === 0 ? 'text-yellow-500' : index === 1 ? 'text-gray-400' : index === 2 ? 'text-orange-400' : 'text-gray-300'">
                      {{ index + 1 }}
                    </span>
                    <div class="flex-1 min-w-0">
                      <h3 class="font-medium text-gray-800 text-sm truncate">{{ stock.isuNm }}</h3>
                      <p class="text-xs text-gray-400">거래량 {{ Number(stock.volume).toLocaleString() }}</p>
                    </div>
                    <div class="text-right flex-shrink-0">
                      <p class="text-sm font-bold text-gray-800">{{ Number(stock.currentPrice).toLocaleString() }}원</p>
                      <p class="text-xs" :class="Number(stock.changeRate) >= 0 ? 'text-green-600' : 'text-red-600'">
                        {{ Number(stock.changeRate) >= 0 ? '+' : '' }}{{ stock.changeRate }}%
                      </p>
                    </div>
                  </div>
                </div>
              </template>
              <!-- 장 마감: DB 기반 거래량 TOP 5 -->
              <template v-else>
                <div
                  v-for="(stock, index) in topVolumeList.slice(0, 5)"
                  :key="stock.isuCd"
                  class="p-3 hover:bg-gray-50 transition-colors duration-200 cursor-pointer"
                  @click="selectStock(stock)"
                >
                  <div class="flex items-center gap-2">
                    <span class="text-xs font-bold w-5 text-center" :class="index === 0 ? 'text-yellow-500' : index === 1 ? 'text-gray-400' : index === 2 ? 'text-orange-400' : 'text-gray-300'">
                      {{ index + 1 }}
                    </span>
                    <div class="flex-1 min-w-0">
                      <h3 class="font-medium text-gray-800 text-sm truncate">{{ stock.isuNm }}</h3>
                      <p class="text-xs text-gray-400">거래량 {{ stock.accTrdvol }}</p>
                    </div>
                    <div class="text-right flex-shrink-0">
                      <p class="text-sm font-bold text-gray-800">{{ stock.tddClsprc }}</p>
                      <p class="text-xs" :class="parseFloat(stock.flucRt) >= 0 ? 'text-green-600' : 'text-red-600'">
                        {{ stock.flucRt }}%
                      </p>
                    </div>
                  </div>
                </div>
                <div v-if="topVolumeList.length === 0" class="p-4 text-center text-gray-500 text-sm">
                  데이터가 없습니다
                </div>
              </template>
            </div>
          </div>
        </div>

        <!-- 종목 검색 -->
        <div class="mt-5 relative">
          <h2 class="text-xl font-bold text-gray-800 mb-2">종목 검색</h2>
          <input
            v-model="searchQuery"
            @input="onSearchInput"
            @blur="closeDropdown"
            type="text"
            placeholder="종목명 검색..."
            class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
          />
          <ul
            v-if="showDropdown"
            class="absolute z-10 w-full bg-white border border-gray-200 rounded-lg shadow-lg mt-1 max-h-52 overflow-y-auto"
          >
            <li
              v-for="stock in searchResults"
              :key="stock.isuCd"
              @mousedown.prevent="selectStock(stock)"
              class="px-3 py-2 hover:bg-blue-50 cursor-pointer text-sm"
            >
              <span class="font-medium text-gray-800">{{ stock.isuNm }}</span>
              <span class="ml-2 text-gray-400 text-xs">{{ stock.isuSrtCd }}</span>
            </li>
          </ul>
        </div>

        <!-- 종목 선택 시 관심 등록 버튼 -->
        <div v-if="selectedIsuSrtCd" class="mt-3 flex items-center justify-between px-1">
          <span class="text-sm font-medium text-gray-700 truncate">{{ selectedIndex }}</span>
          <button
            @click="toggleWatchlist"
            :title="isInWatchlist ? '관심 종목 해제' : '관심 종목 추가'"
            class="flex items-center gap-1 px-2 py-1 rounded-lg transition-colors"
            :class="isInWatchlist ? 'text-amber-500 hover:bg-amber-50' : 'text-gray-400 hover:bg-gray-100'"
          >
            <v-icon size="16">{{ isInWatchlist ? 'mdi-star' : 'mdi-star-outline' }}</v-icon>
            <span class="text-xs">{{ isInWatchlist ? '관심 해제' : '관심 등록' }}</span>
          </button>
        </div>

        <!-- 선택 종목 시세 카드 (실시간 우선, 없으면 DB 종가 fallback) -->
        <div v-if="selectedRealtimePrice || selectedStaticPrice" class="mt-2 bg-white rounded-xl shadow-sm border border-gray-200 p-4">
          <div class="flex justify-between items-center mb-1">
            <div class="flex items-center gap-1.5">
              <h3 class="font-bold text-gray-800 text-sm">
                {{ selectedRealtimePrice ? '실시간 시세' : '전일 종가' }}
              </h3>
              <span v-if="selectedRealtimePrice" class="text-xs bg-green-100 text-green-600 px-1.5 py-0.5 rounded-full font-medium">LIVE</span>
            </div>
            <span v-if="selectedRealtimePrice" class="text-xs text-gray-400">
              {{ selectedRealtimePrice.time.replace(/(\d{2})(\d{2})(\d{2})/, '$1:$2:$3') }}
            </span>
          </div>
          <template v-if="selectedRealtimePrice">
            <div class="flex justify-between items-end">
              <p class="text-2xl font-bold text-gray-900">{{ Number(selectedRealtimePrice.currentPrice).toLocaleString() }}원</p>
              <p :class="Number(selectedRealtimePrice.change) >= 0 ? 'text-green-600 font-semibold' : 'text-red-600 font-semibold'">
                {{ Number(selectedRealtimePrice.change) >= 0 ? '+' : '' }}{{ Number(selectedRealtimePrice.change).toLocaleString() }}
                ({{ selectedRealtimePrice.changeRate }}%)
              </p>
            </div>
            <div class="grid grid-cols-3 gap-2 mt-2 text-xs text-gray-500">
              <div>시 <span class="text-gray-700 font-medium">{{ Number(selectedRealtimePrice.open).toLocaleString() }}</span></div>
              <div>고 <span class="text-green-600 font-medium">{{ Number(selectedRealtimePrice.high).toLocaleString() }}</span></div>
              <div>저 <span class="text-red-600 font-medium">{{ Number(selectedRealtimePrice.low).toLocaleString() }}</span></div>
            </div>
            <p class="text-xs text-gray-400 mt-1">거래량 {{ Number(selectedRealtimePrice.volume).toLocaleString() }}</p>
          </template>
          <template v-else-if="selectedStaticPrice">
            <div class="flex justify-between items-end">
              <p class="text-2xl font-bold text-gray-900">{{ selectedStaticPrice.currentPrice }}원</p>
              <p :class="selectedStaticPrice.changeRate.startsWith('-') ? 'text-red-600 font-semibold' : 'text-green-600 font-semibold'">
                {{ selectedStaticPrice.change }}
                ({{ selectedStaticPrice.changeRate }}%)
              </p>
            </div>
          </template>
        </div>
      </div>

      <!-- 차트 -->
      <div class="lg:col-span-2">
        <div class="flex flex-wrap justify-between items-center mb-4 gap-2">
          <div class="flex items-center gap-2">
            <h2 class="text-xl font-bold text-gray-800">
              {{ selectedMarket === 'STOCK' ? selectedIndex : '시장 동향' }}
            </h2>
            <button
              v-if="selectedMarket === 'STOCK'"
              @click="changeMarket('KOSPI', '코스피')"
              class="text-xs text-gray-400 hover:text-gray-600 underline"
            >시장으로</button>
          </div>
          <div class="flex gap-2 overflow-x-auto pb-0.5">
            <button
              v-for="btn in marketButtons"
              :key="btn.market"
              @click="changeMarket(btn.market, btn.index)"
              :class="[
                'px-4 py-2 rounded-lg font-medium transition-colors flex-shrink-0',
                selectedMarket === btn.market && !compareMode
                  ? 'bg-gray-800 text-white'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              ]"
            >{{ btn.label }}</button>
            <button
              @click="toggleCompare"
              :class="[
                'px-4 py-2 rounded-lg font-medium transition-colors flex-shrink-0',
                compareMode ? 'bg-purple-600 text-white' : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
              ]"
            >비교</button>
          </div>
        </div>

        <div class="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <!-- 기간 선택 + 차트 타입 -->
          <div class="flex flex-col gap-2 mb-4">
            <!-- 1행: 라인/캔들 + 기간 버튼 -->
            <div class="flex items-center gap-1 overflow-x-auto pb-0.5">
              <template v-if="hasOhlc && !compareMode">
                <button
                  @click="chartType = 'line'"
                  :class="['px-3 py-1 text-sm rounded-md font-medium transition-colors whitespace-nowrap flex-shrink-0',
                    chartType === 'line' ? 'bg-gray-800 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200']"
                >라인</button>
                <button
                  @click="chartType = 'candle'"
                  :class="['px-3 py-1 text-sm rounded-md font-medium transition-colors whitespace-nowrap flex-shrink-0',
                    chartType === 'candle' ? 'bg-gray-800 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200']"
                >캔들</button>
                <span class="w-px h-5 bg-gray-200 mx-1 flex-shrink-0"></span>
              </template>
              <button
                v-for="option in periodOptions"
                :key="option.value"
                @click="changePeriod(option.value)"
                :class="['px-3 py-1 text-sm rounded-md font-medium transition-colors whitespace-nowrap flex-shrink-0',
                  selectedPeriod === option.value
                    ? 'bg-gray-800 text-white'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200']"
              >{{ option.label }}</button>
            </div>
            <!-- 2행: 날짜 범위 직접 입력 -->
            <div class="flex items-center gap-1">
              <input type="date" v-model="startDate"
                class="flex-1 min-w-0 px-2 py-1 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-gray-400" />
              <span class="text-gray-500 text-sm flex-shrink-0">~</span>
              <input type="date" v-model="endDate"
                class="flex-1 min-w-0 px-2 py-1 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-gray-400" />
              <button @click="applyDateRange" :disabled="!startDate || !endDate"
                class="flex-shrink-0 px-3 py-1 text-sm bg-gray-800 text-white rounded-md hover:bg-gray-700 disabled:opacity-40 disabled:cursor-not-allowed"
              >조회</button>
              <button v-if="startDate || endDate" @click="clearDateRange"
                class="flex-shrink-0 px-2 py-1 text-sm text-gray-500 hover:text-gray-700"
              >✕</button>
            </div>
          </div>

          <!-- 비교 모드 범례 -->
          <div v-if="compareMode" class="flex items-center gap-5 mb-3 text-sm">
            <span class="flex items-center gap-2">
              <span class="inline-block w-8 h-[3px] rounded bg-blue-500"></span>
              <span class="font-medium text-gray-700">KOSPI</span>
              <span class="text-xs text-gray-400">(좌축)</span>
            </span>
            <span class="flex items-center gap-2">
              <span class="inline-block w-8 h-[3px] rounded bg-orange-500"></span>
              <span class="font-medium text-gray-700">코스닥</span>
              <span class="text-xs text-gray-400">(우축)</span>
            </span>
          </div>

          <!-- 지수 차트 -->
          <div class="h-[350px]">
            <template v-if="chartLabels.length > 0">
              <canvas v-show="chartType === 'candle'" ref="candleCanvasRef" />
              <Line v-if="chartType !== 'candle'" :data="lineChartData" :options="lineChartOptions" />
            </template>
            <div v-else class="h-full flex items-center justify-center text-gray-500">
              차트 데이터가 없습니다. 스케줄러 실행 후 데이터가 수집됩니다.
            </div>
          </div>

          <!-- 거래량 차트 -->
          <div class="h-[150px] mt-4">
            <Bar v-if="chartVolumes.length > 0" :data="volumeChartData" :options="volumeChartOptions" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
