<script setup lang="ts">
import { Line } from 'vue-chartjs'
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend } from 'chart.js'
import { ref, onMounted, computed } from 'vue'
import { fetchRequest } from '../util/fetchRequest'
import { StockDailyTradingDTO } from '../model/DashboardDataDTO'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend)

interface ChartDataDTO {
  labels: string[]
  values: string[]
  indexName: string
}

const selectedMarket = ref('KOSPI')
const selectedIndex = ref('코스피')
const chartLabels = ref<string[]>([])
const chartValues = ref<string[]>([])
const topVolumeList = ref<StockDailyTradingDTO[]>([])

const chartData = computed(() => ({
  labels: chartLabels.value.map(label => {
    // YYYYMMDD -> MM/DD 형식으로 변환
    if (label && label.length === 8) {
      return `${label.substring(4, 6)}/${label.substring(6, 8)}`
    }
    return label
  }),
  datasets: [
    {
      label: selectedIndex.value,
      data: chartValues.value.map(v => parseFloat(v?.replace(/,/g, '') || '0')),
      borderColor: selectedMarket.value === 'KOSPI' ? 'rgb(59, 130, 246)' : 'rgb(234, 88, 12)',
      backgroundColor: selectedMarket.value === 'KOSPI' ? 'rgba(59, 130, 246, 0.1)' : 'rgba(234, 88, 12, 0.1)',
      tension: 0.4,
      fill: true
    }
  ]
}))

const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'top' as const,
      labels: {
        font: {
          size: 14
        }
      }
    },
    title: {
      display: true,
      text: `${selectedIndex.value} 지수 추이`,
      font: {
        size: 16,
        weight: 'bold' as const
      }
    }
  },
  scales: {
    y: {
      beginAtZero: false,
      grid: {
        color: 'rgba(0, 0, 0, 0.1)'
      }
    },
    x: {
      grid: {
        display: false
      }
    }
  }
}))

const fetchChartData = async () => {
  try {
    const data = await fetchRequest<ChartDataDTO>(
      `/dashboard/chart-data?market=${selectedMarket.value}&indexName=${selectedIndex.value}&limit=30`,
      'GET'
    )
    chartLabels.value = data.labels || []
    chartValues.value = data.values || []
  } catch (error) {
    console.error('Error fetching chart data:', error)
  }
}

const fetchTopVolume = async () => {
  try {
    const data = await fetchRequest<{ topVolumeList: StockDailyTradingDTO[] }>('/dashboard/data', 'GET')
    topVolumeList.value = data.topVolumeList || []
  } catch (error) {
    console.error('Error fetching top volume:', error)
  }
}

const changeMarket = (market: string, indexName: string) => {
  selectedMarket.value = market
  selectedIndex.value = indexName
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
      <!-- 거래량 TOP 종목 -->
      <div class="lg:col-span-1">
        <h2 class="text-xl font-bold text-gray-800 mb-4">거래량 TOP</h2>
        <div class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div class="divide-y divide-gray-200">
            <div
              v-for="(stock, index) in topVolumeList"
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
        <div class="flex justify-between items-center mb-4">
          <h2 class="text-xl font-bold text-gray-800">시장 동향</h2>
          <div class="flex gap-2">
            <button
              @click="changeMarket('KOSPI', '코스피')"
              :class="[
                'px-4 py-2 rounded-lg font-medium transition-colors',
                selectedMarket === 'KOSPI'
                  ? 'bg-blue-500 text-white'
                  : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
              ]"
            >
              KOSPI
            </button>
            <button
              @click="changeMarket('KOSDAQ', '코스닥')"
              :class="[
                'px-4 py-2 rounded-lg font-medium transition-colors',
                selectedMarket === 'KOSDAQ'
                  ? 'bg-orange-500 text-white'
                  : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
              ]"
            >
              KOSDAQ
            </button>
          </div>
        </div>
        <div class="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div class="h-[500px]">
            <Line v-if="chartLabels.length > 0" :data="chartData" :options="chartOptions" />
            <div v-else class="h-full flex items-center justify-center text-gray-500">
              차트 데이터가 없습니다. 스케줄러 실행 후 데이터가 수집됩니다.
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>