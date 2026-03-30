<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { Line, Bar } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale, LinearScale, TimeScale, PointElement, LineElement, BarElement,
  Title, Tooltip, Legend, Chart
} from 'chart.js'
import { CandlestickController, CandlestickElement } from 'chartjs-chart-financial'
import 'chartjs-adapter-date-fns'
import { fetchRequest } from '../util/fetchRequest'

ChartJS.register(
  CategoryScale, LinearScale, TimeScale, PointElement, LineElement, BarElement,
  Title, Tooltip, Legend,
  CandlestickController, CandlestickElement
)

// ─── 타입 ────────────────────────────────────────────────────
interface OverseasStockDTO {
  bassDt: string
  isuCd: string
  isuNm: string
  excd: string
  curr: string
  clsPrc: number
  opnPrc: number
  hgstPrc: number
  lwstPrc: number
  accTrdVol: number
  flucRt: number
}

interface OverseasCurrentPriceDTO {
  isuCd: string
  isuNm: string
  excd: string
  curr: string
  lastPrc: number   // 현재가 (USD)
  basePrc: number   // 전일종가 (USD)
  diff: number      // 전일대비
  rate: number      // 등락률 (%)
  sign: string      // 2:상승 3:보합 5:하락
  xrat: number      // 환율 (USD/KRW)
  krwPrc: number    // KRW 환산가
  tvol: number      // 거래량
}

// ─── 인기 종목 전체 목록 (30종목) ────────────────────────────
const ALL_STOCKS = [
  // NASDAQ
  { excd: 'NAS', symb: 'AAPL',  name: 'Apple' },
  { excd: 'NAS', symb: 'MSFT',  name: 'Microsoft' },
  { excd: 'NAS', symb: 'GOOGL', name: 'Alphabet' },
  { excd: 'NAS', symb: 'AMZN',  name: 'Amazon' },
  { excd: 'NAS', symb: 'NVDA',  name: 'NVIDIA' },
  { excd: 'NAS', symb: 'META',  name: 'Meta' },
  { excd: 'NAS', symb: 'TSLA',  name: 'Tesla' },
  { excd: 'NAS', symb: 'AMD',   name: 'AMD' },
  { excd: 'NAS', symb: 'INTC',  name: 'Intel' },
  { excd: 'NAS', symb: 'QCOM',  name: 'Qualcomm' },
  { excd: 'NAS', symb: 'AVGO',  name: 'Broadcom' },
  { excd: 'NAS', symb: 'NFLX',  name: 'Netflix' },
  { excd: 'NAS', symb: 'ADBE',  name: 'Adobe' },
  // NYSE
  { excd: 'NYS', symb: 'ORCL',  name: 'Oracle' },
  { excd: 'NYS', symb: 'UBER',  name: 'Uber' },
  { excd: 'NYS', symb: 'JPM',   name: 'JPMorgan' },
  { excd: 'NYS', symb: 'V',     name: 'Visa' },
  { excd: 'NYS', symb: 'MA',    name: 'Mastercard' },
  { excd: 'NYS', symb: 'BAC',   name: 'Bank of America' },
  { excd: 'NYS', symb: 'GS',    name: 'Goldman Sachs' },
  { excd: 'NYS', symb: 'JNJ',   name: 'Johnson & Johnson' },
  { excd: 'NYS', symb: 'UNH',   name: 'UnitedHealth' },
  { excd: 'NYS', symb: 'PFE',   name: 'Pfizer' },
  { excd: 'NYS', symb: 'KO',    name: 'Coca-Cola' },
  { excd: 'NAS', symb: 'WMT',   name: 'Walmart' },
  { excd: 'NYS', symb: 'HD',    name: 'Home Depot' },
  { excd: 'NYS', symb: 'NKE',   name: 'Nike' },
  { excd: 'NYS', symb: 'XOM',   name: 'Exxon Mobil' },
  { excd: 'NYS', symb: 'CVX',   name: 'Chevron' },
  { excd: 'NYS', symb: 'DIS',   name: 'Disney' },
]

const EXCD_COLOR: Record<string, string> = {
  NAS: 'bg-blue-100 text-blue-700',
  NYS: 'bg-green-100 text-green-700',
  AMS: 'bg-purple-100 text-purple-700',
}

const PAGE_SIZE = 5

// ─── 거래소 필터 + 페이지네이션 ────────────────────────────────
const EXCHANGES = ['ALL', 'NAS', 'NYS', 'AMS']
const selectedExcd = ref('ALL')
const currentPage = ref(0)  // 0-based

const filteredStocks = computed(() =>
  selectedExcd.value === 'ALL'
    ? ALL_STOCKS
    : ALL_STOCKS.filter(s => s.excd === selectedExcd.value)
)

const totalPages = computed(() => Math.ceil(filteredStocks.value.length / PAGE_SIZE))

const pagedStocks = computed(() =>
  filteredStocks.value.slice(currentPage.value * PAGE_SIZE, (currentPage.value + 1) * PAGE_SIZE)
)

const setExcd = (excd: string) => {
  selectedExcd.value = excd
  currentPage.value = 0
}

const prevPage = () => { if (currentPage.value > 0) currentPage.value-- }
const nextPage = () => { if (currentPage.value < totalPages.value - 1) currentPage.value++ }

// ─── 모바일 스와이프 ──────────────────────────────────────────
let touchStartX = 0
const onTouchStart = (e: TouchEvent) => { touchStartX = e.touches[0].clientX }
const onTouchEnd = (e: TouchEvent) => {
  const diff = touchStartX - e.changedTouches[0].clientX
  if (Math.abs(diff) > 50) diff > 0 ? nextPage() : prevPage()
}

// ─── 종목 선택 / 차트 ────────────────────────────────────────
const selectedStock = ref(ALL_STOCKS[0])
const selectedPeriod = ref('3M')
const chartType = ref<'line' | 'candle'>('line')
const chartData = ref<OverseasStockDTO[]>([])
const currentPrice = ref<OverseasCurrentPriceDTO | null>(null)
const priceLoading = ref(false)
const chartLoading = ref(false)

const candleCanvasRef = ref<HTMLCanvasElement | null>(null)
let candleChartInstance: Chart | null = null

const periodOptions = ['1W', '1M', '3M', '6M', '1Y']

// ─── 검색 ─────────────────────────────────────────────────────
const searchQuery = ref('')
const searchResults = ref<OverseasStockDTO[]>([])
const showDropdown = ref(false)
let searchTimer: ReturnType<typeof setTimeout> | null = null

// ─── 차트 계산 ────────────────────────────────────────────────
const chronoData = computed(() => [...chartData.value].reverse())

const chartLabels = computed(() =>
  chronoData.value.map(d =>
    d.bassDt.length === 8
      ? `${d.bassDt.substring(4, 6)}/${d.bassDt.substring(6, 8)}`
      : d.bassDt
  )
)

const lineChartDataset = computed(() => ({
  labels: chartLabels.value,
  datasets: [{
    label: selectedStock.value.symb,
    data: chronoData.value.map(d => Number(d.clsPrc)),
    borderColor: 'rgb(59, 130, 246)',
    backgroundColor: 'rgba(59, 130, 246, 0.1)',
    tension: 0.4,
    fill: true,
  }]
}))

const lineChartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    title: {
      display: true,
      text: `${selectedStock.value.name} (${selectedStock.value.symb})`,
      font: { size: 16, weight: 'bold' as const }
    },
    tooltip: {
      callbacks: {
        label: (ctx: any) => `$${ctx.parsed.y.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
      }
    }
  },
  scales: {
    y: {
      beginAtZero: false,
      grid: { color: 'rgba(0,0,0,0.1)' },
      ticks: { callback: (v: any) => `$${Number(v).toFixed(0)}` }
    },
    x: { grid: { display: false } }
  }
}))

const volumeChartDataset = computed(() => ({
  labels: chartLabels.value,
  datasets: [{
    label: '거래량',
    data: chronoData.value.map(d => Number(d.accTrdVol)),
    backgroundColor: 'rgba(59, 130, 246, 0.5)',
    borderColor: 'rgb(59, 130, 246)',
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

// ─── 캔들 차트 ────────────────────────────────────────────────
const renderCandleChart = () => {
  if (!candleCanvasRef.value) return
  candleChartInstance?.destroy()
  candleChartInstance = null
  candleChartInstance = new Chart(candleCanvasRef.value, {
    type: 'candlestick' as any,
    data: {
      datasets: [{
        label: selectedStock.value.symb,
        data: chronoData.value.map(d => ({
          x: new Date(`${d.bassDt.substring(0,4)}-${d.bassDt.substring(4,6)}-${d.bassDt.substring(6,8)}`).getTime(),
          o: Number(d.opnPrc),
          h: Number(d.hgstPrc),
          l: Number(d.lwstPrc),
          c: Number(d.clsPrc),
        })),
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: 'top' },
        title: { display: true, text: `${selectedStock.value.symb} 캔들차트`, font: { size: 16, weight: 'bold' } }
      },
      scales: {
        x: { grid: { display: false } },
        y: { beginAtZero: false }
      }
    }
  })
}

watch(chartType, async () => {
  if (chartType.value === 'candle') { await nextTick(); renderCandleChart() }
  else { candleChartInstance?.destroy(); candleChartInstance = null }
})

watch(chartData, async () => {
  if (chartType.value === 'candle') { await nextTick(); renderCandleChart() }
})

onBeforeUnmount(() => { candleChartInstance?.destroy() })

// ─── API ──────────────────────────────────────────────────────
const loadChart = async () => {
  chartLoading.value = true
  try {
    chartData.value = await fetchRequest<OverseasStockDTO[]>(
      `/overseas/chart?excd=${selectedStock.value.excd}&symb=${selectedStock.value.symb}&period=${selectedPeriod.value}`,
      'GET'
    ) || []
  } catch { chartData.value = [] }
  finally { chartLoading.value = false }
}

const loadCurrentPrice = async () => {
  priceLoading.value = true
  currentPrice.value = null
  try {
    currentPrice.value = await fetchRequest<OverseasCurrentPriceDTO>(
      `/overseas/price?excd=${selectedStock.value.excd}&symb=${selectedStock.value.symb}&name=${encodeURIComponent(selectedStock.value.name)}`,
      'GET'
    )
  } catch { /* no-op */ }
  finally { priceLoading.value = false }
}

const selectStock = (stock: typeof ALL_STOCKS[0]) => {
  selectedStock.value = stock
  chartType.value = 'line'
  loadChart()
  loadCurrentPrice()
}

const changePeriod = (p: string) => { selectedPeriod.value = p; loadChart() }

const onSearchInput = () => {
  if (searchTimer) clearTimeout(searchTimer)
  if (!searchQuery.value.trim()) { searchResults.value = []; showDropdown.value = false; return }
  searchTimer = setTimeout(async () => {
    try {
      searchResults.value = await fetchRequest<OverseasStockDTO[]>(
        `/overseas/search?keyword=${encodeURIComponent(searchQuery.value)}&limit=10`, 'GET'
      ) || []
      showDropdown.value = searchResults.value.length > 0
    } catch { /* no-op */ }
  }, 300)
}

const selectFromSearch = (item: OverseasStockDTO) => {
  selectedStock.value = { excd: item.excd, symb: item.isuCd, name: item.isuNm }
  searchQuery.value = `${item.isuCd} · ${item.isuNm}`
  showDropdown.value = false
  chartType.value = 'line'
  loadChart()
  loadCurrentPrice()
}

const closeDropdown = () => setTimeout(() => { showDropdown.value = false }, 150)

const formatUsd = (v: number) =>
  `$${Number(v).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`

const flucColor = (v: number) => Number(v) >= 0 ? 'text-green-600' : 'text-red-600'

onMounted(() => { loadChart(); loadCurrentPrice() })
</script>

<template>
  <div class="max-w-6xl mx-auto">
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">

      <!-- ── 사이드 패널 ────────────────────────── -->
      <div class="lg:col-span-1 space-y-5">

        <!-- 거래소 필터 + 종목 리스트 -->
        <div>
          <h2 class="text-xl font-bold text-gray-800 mb-3">인기 종목</h2>

          <!-- 거래소 필터 탭 -->
          <div class="flex gap-1 mb-2">
            <button
              v-for="ex in EXCHANGES"
              :key="ex"
              @click="setExcd(ex)"
              class="px-3 py-1 text-xs font-semibold rounded-lg transition-colors flex-shrink-0"
              :class="selectedExcd === ex
                ? 'bg-gray-800 text-white'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
            >{{ ex }}</button>
          </div>

          <!-- 종목 리스트 (5개씩) — 모바일 스와이프 -->
          <div
            class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden"
            @touchstart.passive="onTouchStart"
            @touchend.passive="onTouchEnd"
          >
            <div class="divide-y divide-gray-100">
              <div
                v-for="stock in pagedStocks"
                :key="`${stock.excd}-${stock.symb}`"
                @click="selectStock(stock)"
                class="flex items-center gap-3 px-4 py-3 hover:bg-blue-50 cursor-pointer transition-colors"
                :class="selectedStock.symb === stock.symb ? 'bg-blue-50' : ''"
              >
                <span
                  class="text-[11px] font-bold px-1.5 py-0.5 rounded flex-shrink-0"
                  :class="EXCD_COLOR[stock.excd] || 'bg-gray-100 text-gray-600'"
                >{{ stock.excd }}</span>
                <div class="flex-1 min-w-0">
                  <p class="font-semibold text-gray-800 text-sm">{{ stock.symb }}</p>
                  <p class="text-xs text-gray-400 truncate">{{ stock.name }}</p>
                </div>
                <v-icon v-if="selectedStock.symb === stock.symb" size="14" color="primary">mdi-chevron-right</v-icon>
              </div>
              <!-- 빈 슬롯 채우기 (레이아웃 고정) -->
              <div
                v-for="i in PAGE_SIZE - pagedStocks.length"
                :key="`empty-${i}`"
                class="px-4 py-3 h-[52px]"
              />
            </div>

            <!-- 페이지네이션 -->
            <div class="flex items-center justify-between px-4 py-2 border-t border-gray-100 bg-gray-50">
              <button
                @click="prevPage"
                :disabled="currentPage === 0"
                class="p-1 rounded transition-colors"
                :class="currentPage === 0 ? 'text-gray-300' : 'text-gray-600 hover:bg-gray-200'"
              >
                <v-icon size="18">mdi-chevron-left</v-icon>
              </button>

              <!-- 페이지 점 -->
              <div class="flex gap-1.5">
                <button
                  v-for="p in totalPages"
                  :key="p"
                  @click="currentPage = p - 1"
                  class="w-2 h-2 rounded-full transition-colors"
                  :class="currentPage === p - 1 ? 'bg-blue-600' : 'bg-gray-300'"
                />
              </div>

              <button
                @click="nextPage"
                :disabled="currentPage >= totalPages - 1"
                class="p-1 rounded transition-colors"
                :class="currentPage >= totalPages - 1 ? 'text-gray-300' : 'text-gray-600 hover:bg-gray-200'"
              >
                <v-icon size="18">mdi-chevron-right</v-icon>
              </button>
            </div>

            <!-- 모바일 스와이프 힌트 -->
            <p class="md:hidden text-center text-[10px] text-gray-300 py-1">← 스와이프로 페이지 이동 →</p>
          </div>
        </div>

        <!-- 종목 검색 -->
        <div class="relative">
          <h2 class="text-xl font-bold text-gray-800 mb-2">종목 검색</h2>
          <input
            v-model="searchQuery"
            @input="onSearchInput"
            @blur="closeDropdown"
            type="text"
            placeholder="티커 또는 종목명 (예: AAPL)"
            class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
          />
          <ul
            v-if="showDropdown"
            class="absolute z-10 w-full bg-white border border-gray-200 rounded-lg shadow-lg mt-1 max-h-52 overflow-y-auto"
          >
            <li
              v-for="item in searchResults"
              :key="`${item.excd}-${item.isuCd}`"
              @mousedown.prevent="selectFromSearch(item)"
              class="flex items-center gap-2 px-3 py-2 hover:bg-blue-50 cursor-pointer text-sm"
            >
              <span
                class="text-[10px] font-bold px-1 py-0.5 rounded flex-shrink-0"
                :class="EXCD_COLOR[item.excd] || 'bg-gray-100 text-gray-600'"
              >{{ item.excd }}</span>
              <span class="font-medium text-gray-800">{{ item.isuCd }}</span>
              <span class="text-gray-400 text-xs truncate">{{ item.isuNm }}</span>
            </li>
          </ul>
        </div>

        <!-- 현재가 카드 -->
        <div class="bg-white rounded-xl shadow-sm border border-gray-200 p-4">
          <div class="flex items-center gap-2 mb-3">
            <span
              class="text-xs font-bold px-2 py-0.5 rounded"
              :class="EXCD_COLOR[selectedStock.excd] || 'bg-gray-100 text-gray-600'"
            >{{ selectedStock.excd }}</span>
            <h3 class="font-bold text-gray-800">{{ selectedStock.symb }}</h3>
            <span class="text-xs text-gray-400">{{ selectedStock.name }}</span>
          </div>

          <div v-if="priceLoading" class="flex items-center gap-2 text-gray-400 text-sm">
            <v-progress-circular indeterminate size="16" width="2" />
            <span>현재가 조회 중...</span>
          </div>

          <template v-else-if="currentPrice">
            <!-- USD 현재가 -->
            <div class="flex items-end justify-between">
              <p class="text-3xl font-bold text-gray-900">{{ formatUsd(currentPrice.lastPrc) }}</p>
              <p class="text-sm font-semibold mb-0.5" :class="flucColor(currentPrice.rate)">
                {{ Number(currentPrice.rate) >= 0 ? '+' : '' }}{{ Number(currentPrice.rate).toFixed(2) }}%
              </p>
            </div>
            <!-- KRW 환산가 -->
            <div v-if="currentPrice.krwPrc" class="flex items-center gap-2 mt-1">
              <p class="text-lg font-semibold text-gray-600">
                ₩{{ Number(currentPrice.krwPrc).toLocaleString() }}
              </p>
              <span class="text-xs text-gray-400">
                (환율 {{ Number(currentPrice.xrat).toLocaleString() }}원)
              </span>
            </div>
            <!-- 전일대비 -->
            <p class="text-xs mt-1" :class="flucColor(currentPrice.diff)">
              전일 {{ formatUsd(currentPrice.basePrc) }} 대비
              {{ Number(currentPrice.diff) >= 0 ? '+' : '' }}{{ formatUsd(Math.abs(Number(currentPrice.diff))) }}
            </p>
            <p class="text-xs text-gray-400 mt-2">거래량 {{ Number(currentPrice.tvol).toLocaleString() }}</p>
          </template>

          <div v-else class="text-sm text-gray-400">데이터 없음 (수집 전)</div>
        </div>
      </div>

      <!-- ── 차트 영역 ──────────────────────────── -->
      <div class="lg:col-span-2">
        <div class="flex flex-wrap justify-between items-center mb-4 gap-2">
          <h2 class="text-xl font-bold text-gray-800">
            {{ selectedStock.symb }} · {{ selectedStock.name }}
          </h2>
          <span
            class="text-xs font-bold px-2 py-1 rounded"
            :class="EXCD_COLOR[selectedStock.excd] || 'bg-gray-100 text-gray-600'"
          >{{ selectedStock.excd }}</span>
        </div>

        <div class="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <!-- 차트 타입 + 기간 버튼 -->
          <div class="flex items-center gap-1 mb-4 overflow-x-auto pb-0.5">
            <button
              @click="chartType = 'line'"
              :class="['px-3 py-1 text-sm rounded-md font-medium transition-colors flex-shrink-0',
                chartType === 'line' ? 'bg-gray-800 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200']"
            >라인</button>
            <button
              @click="chartType = 'candle'"
              :class="['px-3 py-1 text-sm rounded-md font-medium transition-colors flex-shrink-0',
                chartType === 'candle' ? 'bg-gray-800 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200']"
            >캔들</button>
            <span class="w-px h-5 bg-gray-200 mx-1 flex-shrink-0"></span>
            <button
              v-for="p in periodOptions"
              :key="p"
              @click="changePeriod(p)"
              :class="['px-3 py-1 text-sm rounded-md font-medium transition-colors flex-shrink-0',
                selectedPeriod === p ? 'bg-gray-800 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200']"
            >{{ p }}</button>
          </div>

          <!-- 차트 -->
          <div class="h-[350px]">
            <div v-if="chartLoading" class="h-full flex items-center justify-center text-gray-400 gap-2">
              <v-progress-circular indeterminate size="24" width="2" />
              <span>차트 로딩 중...</span>
            </div>
            <template v-else-if="chartData.length > 0">
              <canvas v-if="chartType === 'candle'" ref="candleCanvasRef" />
              <Line v-else :data="lineChartDataset" :options="lineChartOptions" />
            </template>
            <div v-else class="h-full flex flex-col items-center justify-center text-gray-400 text-sm gap-1">
              <span>데이터가 없습니다.</span>
              <span class="text-xs">매일 07:00 KST 자동 수집됩니다.</span>
            </div>
          </div>

          <!-- 거래량 차트 -->
          <div class="h-[130px] mt-4">
            <Bar
              v-if="chartData.length > 0 && !chartLoading"
              :data="volumeChartDataset"
              :options="volumeChartOptions"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
