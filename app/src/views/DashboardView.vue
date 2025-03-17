<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { DashboardDataDTO } from '../model/DashboardDataDTO';
import dashboardAPI from '../api/dashboardAPI';
import { fetchRequest } from '../util/fetchRequest';

const economicIndicators = ref([
  { label: '코스피', value: '2,650.23', change: '+1.2%', isPositive: true },
  { label: '코스닥', value: '850.45', change: '-0.8%', isPositive: false },
  { label: 'USD/KRW', value: '1,320.50', change: '+0.3%', isPositive: true },
  { label: '금값', value: '2,156.80', change: '+0.5%', isPositive: true },
  { label: 'WTI 원유', value: '78.25', change: '-1.2%', isPositive: false },
  { label: '비트코인', value: '65,432', change: '+2.1%', isPositive: true }
])

const topGainers = ref([
  { rank: 1, name: '테슬라', price: '890,000', change: '+8.5%', volume: '1,234,567' },
  { rank: 2, name: '삼성전자', price: '75,800', change: '+6.2%', volume: '12,345,678' },
  { rank: 3, name: 'LG에너지솔루션', price: '456,000', change: '+5.8%', volume: '789,012' },
  { rank: 4, name: 'SK하이닉스', price: '156,000', change: '+5.1%', volume: '5,678,901' },
  { rank: 5, name: '현대차', price: '234,500', change: '+4.7%', volume: '3,456,789' }
])

const marketEvents = ref([
  '한국은행 기준금리 동결 결정',
  '미 연준 FOMC 회의 결과 발표',
  '주요 기업 실적 발표 시즌 시작',
  '국제 유가 상승세 지속'
])

const economicEvents = ref([
  { title: '한국은행 금통위', date: '2024-03-25' },
  { title: 'FOMC 회의', date: '2024-03-26' },
  { title: '삼성전자 실적발표', date: '2024-04-01' },
  { title: '미국 고용지표 발표', date: '2024-04-05' }
])

const dashboardData = ref<DashboardDataDTO | null>(null);

const fetchDashboardData = async() => {
  try {
    const result: DashboardDataDTO = await fetchRequest<DashboardDataDTO>("/dashboard/data", "GET");

    console.log("Dashboard Data:", result);
    dashboardData.value = result;
  } catch (error) {
    console.error("Error fetching dashboard data:", error); 
    throw error;
  }
}

onMounted(() => {
  fetchDashboardData();
})

</script>

<template>
  <div class="max-w-6xl mx-auto">
    <h1 class="text-3xl font-bold text-gray-800 mb-8">대시보드</h1>
    
    <!-- 경제 지표 -->
    <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4 mb-8">
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

    <!-- 상승률 TOP 5 -->
    <div class="bg-white rounded-xl shadow-sm p-6 border border-gray-200 mb-8">
      <h2 class="text-xl font-bold text-gray-800 mb-4">오늘의 상승률 TOP 5</h2>
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead>
            <tr class="border-b border-gray-200">
              <th class="text-left py-3 px-4 text-sm font-medium text-gray-500">순위</th>
              <th class="text-left py-3 px-4 text-sm font-medium text-gray-500">종목명</th>
              <th class="text-right py-3 px-4 text-sm font-medium text-gray-500">현재가</th>
              <th class="text-right py-3 px-4 text-sm font-medium text-gray-500">등락률</th>
              <th class="text-right py-3 px-4 text-sm font-medium text-gray-500">거래량</th>
            </tr>
          </thead>
          <tbody>
            <tr 
              v-for="stock in topGainers" 
              :key="stock.rank"
              class="border-b border-gray-100 hover:bg-gray-50"
            >
              <td class="py-3 px-4 text-sm">{{ stock.rank }}</td>
              <td class="py-3 px-4 font-medium">{{ stock.name }}</td>
              <td class="py-3 px-4 text-right">{{ stock.price }}</td>
              <td class="py-3 px-4 text-right text-green-600">{{ stock.change }}</td>
              <td class="py-3 px-4 text-right text-gray-600">{{ stock.volume }}</td>
            </tr>
          </tbody>
        </table>
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
            v-for="(event, index) in dashboardData" 
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
        <h2 class="text-xl font-bold text-gray-800 mb-4">오늘의 주요 경제 이벤트</h2>
        <ul class="space-y-3">
          <li 
            v-for="(event, index) in economicEvents" 
            :key="index"
            class="flex items-start"
          >
            <span class="inline-block w-2 h-2 mt-2 mr-3 bg-blue-500 rounded-full"></span>
            <span class="text-gray-600">{{ event }}</span>
          </li>
        </ul>
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