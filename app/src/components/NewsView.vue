<script setup lang="ts">
import { ref } from 'vue'

interface NewsItem {
  id: number
  title: string
  content: string
  date: string
  category: string
  source: string
}

const news = ref<NewsItem[]>([
  {
    id: 1,
    title: '금융위, 새로운 금융정책 발표 예정',
    content: '금융위원회는 다음 주 새로운 금융정책을 발표할 예정이며, 이는 시장 안정화에 초점을 맞출 것으로 예상됩니다.',
    date: '2024-03-21 14:30',
    category: '정책',
    source: '금융경제'
  },
  {
    id: 2,
    title: '주요 기업 실적 전망 상향 조정',
    content: '국내 주요 기업들의 1분기 실적 전망이 상향 조정되면서 증시에 긍정적인 영향을 미칠 것으로 전망됩니다.',
    date: '2024-03-21 11:20',
    category: '기업분석',
    source: '증권뉴스'
  },
  {
    id: 3,
    title: '글로벌 원자재 가격 동향',
    content: '국제 유가와 원자재 가격의 변동성이 확대되면서 관련 기업들의 수익성에 영향을 미칠 것으로 예상됩니다.',
    date: '2024-03-21 09:45',
    category: '국제경제',
    source: '글로벌경제'
  }
])
</script>

<template>
  <div class="max-w-4xl mx-auto">
    <h2 class="text-2xl font-bold text-gray-800 mb-6">최신 금융뉴스</h2>
    
    <div class="space-y-6">
      <div 
        v-for="item in news" 
        :key="item.id" 
        class="bg-white rounded-xl shadow-sm border border-gray-200 p-6 hover:shadow-md transition-shadow duration-200"
      >
        <div class="flex items-center justify-between mb-3">
          <span class="px-3 py-1 bg-blue-100 text-blue-700 rounded-full text-sm font-medium">
            {{ item.category }}
          </span>
          <div class="text-sm text-gray-500">
            {{ item.source }} · {{ item.date }}
          </div>
        </div>
        
        <h3 class="text-xl font-bold text-gray-800 mb-3">{{ item.title }}</h3>
        <p class="text-gray-600 leading-relaxed">{{ item.content }}</p>
      </div>
    </div>
  </div>
</template>