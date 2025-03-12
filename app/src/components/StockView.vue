<script setup lang="ts">
import { Line } from 'vue-chartjs'
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend } from 'chart.js'
import { ref } from 'vue'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend)

const chartData = {
  labels: ['1월', '2월', '3월', '4월', '5월', '6월'],
  datasets: [
    {
      label: 'KOSPI',
      data: [2500, 2550, 2600, 2580, 2620, 2650],
      borderColor: 'rgb(59, 130, 246)',
      backgroundColor: 'rgba(59, 130, 246, 0.1)',
      tension: 0.4,
      fill: true
    }
  ]
}

const chartOptions = {
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
      text: 'KOSPI 지수 추이',
      font: {
        size: 16,
        weight: 'bold'
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
}

const stockInfo = ref([
  { name: '삼성전자', price: '75,800', change: '+1.2%', volume: '12,345,678' },
  { name: 'SK하이닉스', price: '156,000', change: '-0.8%', volume: '5,678,901' },
  { name: 'NAVER', price: '234,500', change: '+2.1%', volume: '3,456,789' },
  { name: '카카오', price: '89,700', change: '+0.5%', volume: '4,567,890' }
])
</script>

<template>
  <div class="max-w-6xl mx-auto">
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
      <!-- 주요 종목 정보 -->
      <div class="lg:col-span-1">
        <h2 class="text-xl font-bold text-gray-800 mb-4">주요 종목</h2>
        <div class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div class="divide-y divide-gray-200">
            <div 
              v-for="(stock, index) in stockInfo" 
              :key="index"
              class="p-4 hover:bg-gray-50 transition-colors duration-200"
            >
              <div class="flex justify-between items-center">
                <div>
                  <h3 class="font-medium text-gray-800">{{ stock.name }}</h3>
                  <p class="text-sm text-gray-500">거래량: {{ stock.volume }}</p>
                </div>
                <div class="text-right">
                  <p class="font-bold text-gray-800">{{ stock.price }}</p>
                  <p :class="stock.change.startsWith('+') ? 'text-green-600' : 'text-red-600'">
                    {{ stock.change }}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 차트 -->
      <div class="lg:col-span-2">
        <h2 class="text-xl font-bold text-gray-800 mb-4">시장 동향</h2>
        <div class="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div class="h-[500px]">
            <Line :data="chartData" :options="chartOptions" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>