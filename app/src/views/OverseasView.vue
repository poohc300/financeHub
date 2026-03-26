<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { Line, Bar } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale, LinearScale, PointElement, LineElement, BarElement,
  Title, Tooltip, Legend, Chart
} from 'chart.js'
import { CandlestickController, CandlestickElement } from 'chartjs-chart-financial'
import { fetchRequest } from '../util/fetchRequest'

ChartJS.register(
  CategoryScale, LinearScale, PointElement, LineElement, BarElement,
  Title, Tooltip, Legend,
  CandlestickController, CandlestickElement
)

// ─── 타입 정의 ───────────────────────────────────────────────
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

// ─── 인기 종목 사이드바 ────────────────────────────────────────
const POPULAR = [
  { excd: 'NAS', symb: 'AAPL',  name: 'Apple' },
  { excd: 'NAS', symb: 'MSFT',  name: 'Microsoft' },
  { excd: 'NAS', symb: 'GOOGL', name: 'Alphabet' },
  { excd: 'NAS', symb: 'AMZN',  name: 'Amazon' },
  { excd: 'NAS', symb: 'TSLA',  name: 'Tesla' },
  { excd: 'NAS', symb: 'NVDA',  name: 'NVIDIA' },
  { excd: 'NAS', symb: 'META',  name: 'Meta' },
  { excd: 'NAS', symb: 'AMD',   name: 'AMD' },
  { excd: 'NAS', symb: 'NFLX',  name: 'Netflix' },
  { excd: 'NYS', symb: 'JPM',   name: 'JPMorgan' },
  { excd: 'NYS', symb: 'V',     name: 'Visa' },
  { excd: 'NYS', symb: 'MA',    name: 'Mastercard' },
]

const EXCD_COLOR: Record<string, string> = {
  NAS: 'bg-blue-100 text-blue-700',
  NYS: 'bg-green-100 text-green-700',
  AMS: 'bg-purple-100 text-purple-700',
}

// ─── 상태 ────────────────────────────────────────────────────
const selectedStock = ref(POPULAR[0])
const selectedPeriod = ref('3M')
const chartType = ref<'line' | 'candle'>('line')

const chartData = ref<OverseasStockDTO[]>([])
const currentPrice = ref<OverseasStockDTO | null>(null)
const priceLoading = ref(false)
const chartLoading = ref(false)

const searchQuery = ref('')
const searchResults = ref<OverseasStockDTO[]>([])
const showDropdown = ref(false)
let searchTimer: ReturnType<typeof setTimeout> | null = null

const candleCanvasRef = ref<HTMLCanvasElement | null>(null)
let candleChartInstance: Chart | null = null

const periodOptions = ['1W', '1M', '3M', '6M', '1Y']

// ─── 차트 데이터 계산 ─────────────────────────────────────────
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
      text: `${selectedStock.value.name} (${selectedStock.value.symb}) 추이`,
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
  if (chartType.value === 'candle') {
    await nextTick()
    renderCandleChart()
  } else {
    candleChartInstance?.destroy()
    candleChartInstance = null
  }
})

watch(chartData, async () => {
  if (chartType.value === 'candle') {
    await nextTick()
    renderCandleChart()
  }
})

onBeforeUnmount(() => { candleChartInstance?.destroy() })

// ─── API 호출 ─────────────────────────────────────────────────
const loadChart = async () => {
  chartLoading.value = true
  try {
    const data = await fetchRequest<OverseasStockDTO[]>(
      `/overseas/chart?excd=${selectedStock.value.excd}&symb=${selectedStock.value.symb}&period=${selectedPeriod.value}`,
      'GET'
    )
    chartData.value = data || []
  } catch (e) {
    console.error('차트 조회 오류:', e)
    chartData.value = []
  } finally {
    chartLoading.value = false
  }
}

const loadCurrentPrice = async () => {
  priceLoading.value = true
  currentPrice.value = null
  try {
    const data = await fetchRequest<OverseasStockDTO>(
      `/overseas/price?excd=${selectedStock.value.excd}&symb=${selectedStock.value.symb}&name=${encodeURIComponent(selectedStock.value.name)}`,
      'GET'
    )
    currentPrice.value = data
  } catch (e) {
    console.error('현재가 조회 오류:', e)
  } finally {
    priceLoading.value = false
  }
}

const selectStock = (stock: typeof POPULAR[0]) => {
  selectedStock.value = stock
  chartType.value = 'line'
  loadChart()
  loadCurrentPrice()
}

const changePeriod = (period: string) => {
  selectedPeriod.value = period
  loadChart()
}

// ─── 검색 ─────────────────────────────────────────────────────
const onSearchInput = () => {
  if (searchTimer) clearTimeout(searchTimer)
  if (!searchQuery.value.trim()) {
    searchResults.value = []
    showDropdown.value = false
    return
  }
  searchTimer = setTimeout(async () => {
    try {
      const results = await fetchRequest<OverseasStockDTO[]>(
        `/overseas/search?keyword=${encodeURIComponent(searchQuery.value)}&limit=10`,
        'GET'
      )
      searchResults.value = results || []
      showDropdown.value = searchResults.value.length > 0
    } catch (e) {
      console.error('검색 오류:', e)
    }
  }, 300)
}

const selectFromSearch = (item: OverseasStockDTO) => {
  const stock = { excd: item.excd, symb: item.isuCd, name: item.isuNm }
  selectedStock.value = stock
  searchQuery.value = `${item.isuCd} - ${item.isuNm}`
  showDropdown.value = false
  chartType.value = 'line'
  loadChart()
  loadCurrentPrice()
}

const closeDropdown = () => setTimeout(() => { showDropdown.value = false }, 150)

// ─── 가격 포맷 ────────────────────────────────────────────────
const formatUsd = (v: number) =>
  `$${Number(v).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`

const flucColor = (v: number) => Number(v) >= 0 ? 'text-green-600' : 'text-red-600'

onMounted(() => {
  loadChart()
  loadCurrentPrice()
})
</script>

<template>
  <div class="max-w-6xl mx-auto">
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">

      <!-- ── 사이드 패널 ────────────────────────── -->
      <div class="lg:col-span-1 space-y-5">

        <!-- 인기 종목 리스트 -->
        <div>
          <h2 class="text-xl font-bold text-gray-800 mb-3">인기 종목</h2>
          <div class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
            <div class="divide-y divide-gray-100">
              <div
                v-for="stock in POPULAR"
                :key="`${stock.excd}-${stock.symb}`"
                @click="selectStock(stock)"
                class="flex items-center gap-3 px-4 py-2.5 hover:bg-blue-50 cursor-pointer transition-colors"
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
            </div>
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

          <!-- 로딩 -->
          <div v-if="priceLoading" class="flex items-center gap-2 text-gray-400 text-sm">
            <v-progress-circular indeterminate size="16" width="2" />
            <span>현재가 조회 중...</span>
          </div>

          <!-- 가격 표시 -->
          <template v-else-if="currentPrice">
            <p class="text-3xl font-bold text-gray-900">{{ formatUsd(currentPrice.clsPrc) }}</p>
            <p class="mt-1 text-sm font-semibold" :class="flucColor(currentPrice.flucRt)">
              {{ Number(currentPrice.flucRt) >= 0 ? '+' : '' }}{{ Number(currentPrice.flucRt).toFixed(2) }}%
            </p>
            <div class="grid grid-cols-3 gap-2 mt-3 text-xs text-gray-500">
              <div>시 <span class="text-gray-700 font-medium">{{ formatUsd(currentPrice.opnPrc) }}</span></div>
              <div>고 <span class="text-green-600 font-medium">{{ formatUsd(currentPrice.hgstPrc) }}</span></div>
              <div>저 <span class="text-red-600 font-medium">{{ formatUsd(currentPrice.lwstPrc) }}</span></div>
            </div>
            <p class="text-xs text-gray-400 mt-2">거래량 {{ Number(currentPrice.accTrdVol).toLocaleString() }}</p>
            <p class="text-xs text-gray-300 mt-1">기준일 {{ currentPrice.bassDt }}</p>
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
            <div v-else class="h-full flex items-center justify-center text-gray-400 text-sm">
              데이터가 없습니다.<br>
              <span class="text-xs mt-1">스케줄러 실행(07:00 KST) 후 수집됩니다.</span>
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
