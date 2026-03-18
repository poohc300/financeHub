<script setup lang="ts">
import { Line, Bar } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale, LinearScale, PointElement, LineElement, BarElement,
  Title, Tooltip, Legend, Chart
} from 'chart.js'
import {
  CandlestickController, CandlestickElement
} from 'chartjs-chart-financial'
import { ref, onMounted, computed, watch, onBeforeUnmount } from 'vue'
import { fetchRequest } from '../util/fetchRequest'
import { StockDailyTradingDTO } from '../model/DashboardDataDTO'

ChartJS.register(
  CategoryScale, LinearScale, PointElement, LineElement, BarElement,
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

const selectedMarket = ref('KOSPI')
const selectedIndex = ref('코스피')
const selectedPeriod = ref(30)
const startDate = ref('')
const endDate = ref('')
const chartType = ref<'line' | 'candle'>('line')
const compareMode = ref(false)

const searchQuery = ref('')
const searchResults = ref<StockDailyTradingDTO[]>([])
const showDropdown = ref(false)
const selectedIsuCd = ref('')
let searchTimer: ReturnType<typeof setTimeout> | null = null

const chartLabels = ref<string[]>([])
const chartValues = ref<string[]>([])
const chartVolumes = ref<string[]>([])
const chartOpens = ref<string[]>([])
const chartHighs = ref<string[]>([])
const chartLows = ref<string[]>([])
const compareValues = ref<string[]>([])
const topVolumeList = ref<StockDailyTradingDTO[]>([])

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
  { label: '금', market: 'GOLD', index: '금현물' },
  { label: '오일', market: 'OIL', index: '두바이유' },
]

const marketColor = computed(() => {
  const map: Record<string, string> = {
    KOSPI: '59, 130, 246',
    KOSDAQ: '234, 88, 12',
    GOLD: '202, 138, 4',
    OIL: '107, 114, 128',
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
    legend: { position: 'top' as const },
    title: { display: true, text: `${selectedIndex.value} 추이`, font: { size: 16, weight: 'bold' as const } }
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
        x: { grid: { display: false } },
        y: { beginAtZero: false }
      }
    }
  })
}

watch([chartType, chartLabels], () => {
  if (chartType.value === 'candle') {
    setTimeout(renderCandleChart, 50)
  } else {
    candleChartInstance?.destroy()
    candleChartInstance = null
  }
})

onBeforeUnmount(() => {
  candleChartInstance?.destroy()
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
  searchQuery.value = stock.isuNm
  showDropdown.value = false
  changeMarket('STOCK', stock.isuNm)
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
  if (market === 'OIL' || market === 'GOLD') chartType.value = 'line'
  fetchChartData()
}

const changePeriod = (period: number) => {
  startDate.value = ''
  endDate.value = ''
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
  startDate.value = ''
  endDate.value = ''
  selectedPeriod.value = 30
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
  fetchChartData()
  fetchTopVolume()
})
</script>

<template>
  <div class="max-w-6xl mx-auto">
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
      <!-- 종목 검색 + 거래량 TOP -->
      <div class="lg:col-span-1">
        <!-- 종목 검색 -->
        <div class="mb-4 relative">
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

        <h2 class="text-xl font-bold text-gray-800 mb-4">거래량 TOP</h2>
        <div class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div class="divide-y divide-gray-200">
            <div
              v-for="(stock) in topVolumeList"
              :key="stock.isuCd"
              class="p-4 hover:bg-gray-50 transition-colors duration-200"
            >
              <div class="flex justify-between items-center">
                <div>
                  <h3 class="font-medium text-gray-800">{{ stock.isuNm }}</h3>
                  <p class="text-sm text-gray-500">거래량: {{ stock.accTrdvol }}</p>
                </div>
                <div class="text-right">
                  <p class="font-bold text-gray-800">{{ stock.tddClsprc }}</p>
                  <p :class="parseFloat(stock.flucRt) >= 0 ? 'text-green-600' : 'text-red-600'">
                    {{ stock.flucRt }}%
                  </p>
                </div>
              </div>
            </div>
            <div v-if="topVolumeList.length === 0" class="p-4 text-center text-gray-500">
              데이터가 없습니다
            </div>
          </div>
        </div>
      </div>

      <!-- 차트 -->
      <div class="lg:col-span-2">
        <div class="flex flex-wrap justify-between items-center mb-4 gap-2">
          <h2 class="text-xl font-bold text-gray-800">시장 동향</h2>
          <div class="flex gap-2 overflow-x-auto pb-0.5">
            <button
              v-for="btn in marketButtons"
              :key="btn.market"
              @click="changeMarket(btn.market, btn.index)"
              :class="[
                'px-4 py-2 rounded-lg font-medium transition-colors flex-shrink-0',
                selectedMarket === btn.market && !compareMode
                  ? 'bg-gray-800 text-white'
                  : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
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
          <div class="flex flex-wrap justify-between gap-2 mb-4">
            <div class="flex gap-1">
              <template v-if="hasOhlc && !compareMode">
                <button
                  @click="chartType = 'line'"
                  :class="['px-3 py-1 text-sm rounded-md font-medium transition-colors',
                    chartType === 'line' ? 'bg-gray-800 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200']"
                >라인</button>
                <button
                  @click="chartType = 'candle'"
                  :class="['px-3 py-1 text-sm rounded-md font-medium transition-colors',
                    chartType === 'candle' ? 'bg-gray-800 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200']"
                >캔들</button>
              </template>
            </div>
            <div class="flex flex-wrap gap-2 justify-end">
              <div class="flex gap-1">
                <button
                  v-for="option in periodOptions"
                  :key="option.value"
                  @click="changePeriod(option.value)"
                  :class="['px-3 py-1 text-sm rounded-md font-medium transition-colors',
                    selectedPeriod === option.value && !startDate
                      ? 'bg-gray-800 text-white'
                      : 'bg-gray-100 text-gray-600 hover:bg-gray-200']"
                >{{ option.label }}</button>
              </div>
              <div class="flex items-center gap-1">
                <input type="date" v-model="startDate"
                  class="px-2 py-1 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-gray-400" />
                <span class="text-gray-500 text-sm">~</span>
                <input type="date" v-model="endDate"
                  class="px-2 py-1 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-gray-400" />
                <button @click="applyDateRange" :disabled="!startDate || !endDate"
                  class="px-3 py-1 text-sm bg-gray-800 text-white rounded-md hover:bg-gray-700 disabled:opacity-40 disabled:cursor-not-allowed"
                >조회</button>
                <button v-if="startDate || endDate" @click="clearDateRange"
                  class="px-2 py-1 text-sm text-gray-500 hover:text-gray-700"
                >✕</button>
              </div>
            </div>
          </div>

          <!-- 지수 차트 -->
          <div class="h-[350px]">
            <template v-if="chartLabels.length > 0">
              <canvas v-if="chartType === 'candle'" ref="candleCanvasRef" />
              <Line v-else :data="lineChartData" :options="lineChartOptions" />
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
