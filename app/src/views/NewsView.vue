<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { fetchRequest } from '../util/fetchRequest'
import { CrawledNewsDTO, DashboardDataDTO } from '../model/DashboardDataDTO'

const newsList = ref<CrawledNewsDTO[]>([])
const loading = ref(true)

const fetchNews = async () => {
  try {
    const data = await fetchRequest<DashboardDataDTO>('/dashboard/data', 'GET')
    newsList.value = data.crawledNewsList || []
  } catch (e) {
    console.error('뉴스 조회 오류:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchNews()
})
</script>

<template>
  <div class="max-w-4xl mx-auto">
    <h2 class="text-2xl font-bold text-gray-800 mb-6">최신 금융뉴스</h2>

    <div v-if="loading" class="text-center text-gray-500 py-12">로딩 중...</div>

    <div v-else-if="newsList.length === 0" class="text-center text-gray-500 py-12">
      뉴스 데이터가 없습니다. 스케줄러 실행 후 데이터가 수집됩니다.
    </div>

    <div v-else class="space-y-4">
      <a
        v-for="(item, index) in newsList"
        :key="index"
        :href="item.link"
        target="_blank"
        rel="noopener noreferrer"
        class="block bg-white rounded-xl shadow-sm border border-gray-200 p-5 hover:shadow-md hover:border-blue-300 transition-all duration-200"
      >
        <div class="flex items-start justify-between gap-4">
          <h3 class="text-base font-semibold text-gray-800 leading-snug">{{ item.title }}</h3>
          <span class="text-xs text-gray-400 whitespace-nowrap mt-1">{{ item.publishedAt }}</span>
        </div>
      </a>
    </div>
  </div>
</template>
