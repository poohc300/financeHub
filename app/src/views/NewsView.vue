<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { fetchRequest } from '../util/fetchRequest'
import { openLink } from '../util/openLink'

interface NewsItem {
  title: string
  link: string
  publishedAt: string
  createdAt: string
  dataHash: string
}

const newsList = ref<NewsItem[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)

// 필터 상태
const selectedPeriod = ref<string>('today')
const searchKeyword = ref<string>('')
const offset = ref(0)
const PAGE_SIZE = 30

// 기간 옵션
const periodOptions = [
  { label: '오늘', value: 'today' },
  { label: '3일', value: '3days' },
  { label: '1주일', value: '1week' },
  { label: '1개월', value: '1month' },
  { label: '전체', value: 'all' },
]

// 주제 키워드 프리셋
const topicPresets = [
  { label: '전체', keyword: '' },
  { label: '증시', keyword: '증시' },
  { label: '코스피', keyword: '코스피' },
  { label: '환율', keyword: '환율' },
  { label: '금리', keyword: '금리' },
  { label: '부동산', keyword: '부동산' },
  { label: '암호화폐', keyword: '비트코인' },
]
const selectedTopic = ref('')

const getDateRange = () => {
  const today = new Date()
  const fmt = (d: Date) => d.toISOString().slice(0, 10)
  const end = fmt(today)

  switch (selectedPeriod.value) {
    case 'today': return { startDate: end, endDate: end }
    case '3days': {
      const s = new Date(today); s.setDate(s.getDate() - 3)
      return { startDate: fmt(s), endDate: end }
    }
    case '1week': {
      const s = new Date(today); s.setDate(s.getDate() - 7)
      return { startDate: fmt(s), endDate: end }
    }
    case '1month': {
      const s = new Date(today); s.setMonth(s.getMonth() - 1)
      return { startDate: fmt(s), endDate: end }
    }
    default: return { startDate: '', endDate: '' }
  }
}

const buildUrl = (currentOffset: number) => {
  const { startDate, endDate } = getDateRange()
  const params = new URLSearchParams()
  if (startDate) params.set('startDate', startDate)
  if (endDate) params.set('endDate', endDate)
  const kw = searchKeyword.value.trim() || selectedTopic.value
  if (kw) params.set('keyword', kw)
  params.set('limit', String(PAGE_SIZE))
  params.set('offset', String(currentOffset))
  return `/news/list?${params.toString()}`
}

const fetchNews = async (reset = true) => {
  if (reset) {
    loading.value = true
    offset.value = 0
    newsList.value = []
  } else {
    loadingMore.value = true
  }

  try {
    const data = await fetchRequest<NewsItem[]>(buildUrl(offset.value), 'GET')
    const items = data || []
    newsList.value = reset ? items : [...newsList.value, ...items]
    hasMore.value = items.length === PAGE_SIZE
    offset.value += items.length
  } catch (e) {
    console.error('뉴스 조회 오류:', e)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const loadMore = () => {
  if (!loadingMore.value && hasMore.value) fetchNews(false)
}

const onPeriodChange = (v: string) => {
  selectedPeriod.value = v
  fetchNews()
}

const onTopicChange = (kw: string) => {
  selectedTopic.value = kw
  searchKeyword.value = ''
  fetchNews()
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
const onSearchInput = () => {
  selectedTopic.value = ''
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => fetchNews(), 400)
}

const formatTime = (createdAt: string) => {
  if (!createdAt) return ''
  return createdAt.slice(11, 16) // HH:MM
}

onMounted(() => fetchNews())
</script>

<template>
  <v-container fluid class="pa-4" style="max-width: 960px;">

        <!-- 헤더 -->
        <div class="mb-4">
          <h1 class="text-h5 font-weight-bold text-grey-darken-3">금융 뉴스</h1>
          <p class="text-body-2 text-grey mt-1">매 1시간마다 자동 수집 · 중복 제거</p>
        </div>

        <!-- 필터 바 -->
        <v-card class="mb-4 pa-3" elevation="1" rounded="lg">
          <!-- 기간 선택 -->
          <div class="mb-3">
            <p class="text-caption text-grey mb-1 font-weight-medium">기간</p>
            <v-btn-toggle
              v-model="selectedPeriod"
              mandatory
              color="primary"
              variant="outlined"
              density="compact"
              rounded="lg"
              @update:modelValue="onPeriodChange"
            >
              <v-btn
                v-for="opt in periodOptions"
                :key="opt.value"
                :value="opt.value"
                size="small"
              >{{ opt.label }}</v-btn>
            </v-btn-toggle>
          </div>

          <!-- 주제 프리셋 -->
          <div class="mb-3">
            <p class="text-caption text-grey mb-1 font-weight-medium">주제</p>
            <div class="d-flex flex-wrap gap-2">
              <v-chip
                v-for="topic in topicPresets"
                :key="topic.keyword"
                :color="selectedTopic === topic.keyword && !searchKeyword ? 'primary' : 'default'"
                :variant="selectedTopic === topic.keyword && !searchKeyword ? 'flat' : 'tonal'"
                size="small"
                @click="onTopicChange(topic.keyword)"
                style="cursor: pointer;"
              >{{ topic.label }}</v-chip>
            </div>
          </div>

          <!-- 검색 -->
          <v-text-field
            v-model="searchKeyword"
            @input="onSearchInput"
            placeholder="키워드 검색..."
            prepend-inner-icon="mdi-magnify"
            clearable
            hide-details
            density="compact"
            variant="outlined"
            rounded="lg"
            @click:clear="() => { searchKeyword = ''; fetchNews() }"
          />
        </v-card>

        <!-- 로딩 -->
        <div v-if="loading" class="d-flex justify-center py-10">
          <v-progress-circular indeterminate color="primary" size="48" />
        </div>

        <!-- 뉴스 없음 -->
        <v-card v-else-if="!loading && newsList.length === 0" elevation="0" class="text-center py-12" color="transparent">
          <v-icon size="48" color="grey-lighten-1">mdi-newspaper-variant-outline</v-icon>
          <p class="text-body-1 text-grey mt-3">뉴스 데이터가 없습니다.</p>
          <p class="text-caption text-grey">스케줄러가 실행되면 자동으로 수집됩니다.</p>
        </v-card>

        <!-- 뉴스 리스트 -->
        <div v-else>
          <v-card
            v-for="(item, index) in newsList"
            :key="item.dataHash || index"
            @click="openLink(item.link)"
            class="mb-3"
            elevation="1"
            rounded="lg"
            hover
            style="cursor: pointer;"
          >
            <v-card-text class="py-3 px-4">
              <div class="d-flex align-start justify-space-between gap-3">
                <div class="flex-grow-1">
                  <p class="text-body-2 font-weight-medium text-grey-darken-3 news-title">
                    {{ item.title }}
                  </p>
                </div>
                <div class="text-right flex-shrink-0">
                  <v-chip
                    v-if="item.createdAt"
                    size="x-small"
                    color="grey"
                    variant="tonal"
                    class="mb-1"
                  >{{ formatTime(item.createdAt) }}</v-chip>
                  <p v-if="item.publishedAt" class="text-caption text-grey">{{ item.publishedAt }}</p>
                </div>
              </div>
            </v-card-text>
          </v-card>

          <!-- 더 보기 -->
          <div class="text-center mt-2 mb-6">
            <v-btn
              v-if="hasMore"
              @click="loadMore"
              :loading="loadingMore"
              variant="tonal"
              color="primary"
              rounded="lg"
            >더 보기</v-btn>
            <p v-else class="text-caption text-grey py-4">모든 뉴스를 불러왔습니다.</p>
          </div>
        </div>

  </v-container>
</template>

<style scoped>
.news-title {
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
