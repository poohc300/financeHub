<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { fetchRequest } from '../util/fetchRequest'

interface IpoItem {
  companyName: string
  link: string
  period: string
  fixedOfferingPrice: string
  expectedOfferingPrice: string
  subscriptionRate: string
  underWriter: string
  createdAt: string
  dataHash: string
}

const ipoList = ref<IpoItem[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)

const searchKeyword = ref('')
const selectedPeriod = ref('all')
const offset = ref(0)
const PAGE_SIZE = 20

const periodOptions = [
  { label: '이번달', value: 'month' },
  { label: '3개월', value: '3months' },
  { label: '전체', value: 'all' },
]

const getDateRange = () => {
  const today = new Date()
  const fmt = (d: Date) => d.toISOString().slice(0, 10)
  const end = fmt(today)
  switch (selectedPeriod.value) {
    case 'month': {
      const s = new Date(today.getFullYear(), today.getMonth(), 1)
      return { startDate: fmt(s), endDate: end }
    }
    case '3months': {
      const s = new Date(today); s.setMonth(s.getMonth() - 3)
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
  if (searchKeyword.value.trim()) params.set('keyword', searchKeyword.value.trim())
  params.set('limit', String(PAGE_SIZE))
  params.set('offset', String(currentOffset))
  return `/ipo/list?${params.toString()}`
}

const fetchIpo = async (reset = true) => {
  if (reset) {
    loading.value = true
    offset.value = 0
    ipoList.value = []
  } else {
    loadingMore.value = true
  }
  try {
    const data = await fetchRequest<IpoItem[]>(buildUrl(offset.value), 'GET')
    const items = data || []
    ipoList.value = reset ? items : [...ipoList.value, ...items]
    hasMore.value = items.length === PAGE_SIZE
    offset.value += items.length
  } catch (e) {
    console.error('공모주 조회 오류:', e)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const loadMore = () => {
  if (!loadingMore.value && hasMore.value) fetchIpo(false)
}

const onPeriodChange = (v: string) => {
  selectedPeriod.value = v
  fetchIpo()
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
const onSearchInput = () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => fetchIpo(), 400)
}

onMounted(() => fetchIpo())
</script>

<template>
  <v-app style="background: #f5f5f5;">
    <v-main>
      <v-container fluid class="pa-4" style="max-width: 1000px;">

        <!-- 헤더 -->
        <div class="mb-4">
          <h1 class="text-h5 font-weight-bold text-grey-darken-3">공모주 일정</h1>
          <p class="text-body-2 text-grey mt-1">최신순 · 매일 자동 수집</p>
        </div>

        <!-- 필터 바 -->
        <v-card class="mb-4 pa-3" elevation="1" rounded="lg">
          <div class="d-flex flex-wrap align-center gap-3">
            <!-- 기간 -->
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

            <!-- 키워드 검색 -->
            <v-text-field
              v-model="searchKeyword"
              @input="onSearchInput"
              placeholder="종목명 검색..."
              prepend-inner-icon="mdi-magnify"
              clearable
              hide-details
              density="compact"
              variant="outlined"
              rounded="lg"
              style="max-width: 260px;"
              @click:clear="() => { searchKeyword = ''; fetchIpo() }"
            />
          </div>
        </v-card>

        <!-- 로딩 -->
        <div v-if="loading" class="d-flex justify-center py-10">
          <v-progress-circular indeterminate color="primary" size="48" />
        </div>

        <!-- 데이터 없음 -->
        <v-card v-else-if="!loading && ipoList.length === 0" elevation="0" class="text-center py-12" color="transparent">
          <v-icon size="48" color="grey-lighten-1">mdi-calendar-blank-outline</v-icon>
          <p class="text-body-1 text-grey mt-3">공모주 데이터가 없습니다.</p>
          <p class="text-caption text-grey">스케줄러 실행 후 자동 수집됩니다.</p>
        </v-card>

        <!-- 공모주 리스트 -->
        <div v-else>
          <v-card
            v-for="(item, index) in ipoList"
            :key="item.dataHash || index"
            class="mb-3"
            elevation="1"
            rounded="lg"
          >
            <v-card-text class="py-3 px-4">
              <div class="d-flex align-start justify-space-between gap-3">

                <!-- 좌측: 종목명 + 주간사 -->
                <div class="flex-grow-1">
                  <div class="d-flex align-center gap-2 mb-1">
                    <a
                      v-if="item.link"
                      :href="item.link"
                      target="_blank"
                      rel="noopener noreferrer"
                      class="text-body-1 font-weight-bold text-blue-darken-2 text-decoration-none"
                      style="hover: underline;"
                    >{{ item.companyName }}</a>
                    <span v-else class="text-body-1 font-weight-bold text-grey-darken-3">{{ item.companyName }}</span>
                  </div>
                  <p v-if="item.underWriter" class="text-caption text-grey mb-0">
                    <v-icon size="12" class="mr-1">mdi-bank-outline</v-icon>{{ item.underWriter }}
                  </p>
                </div>

                <!-- 우측: 수집일 -->
                <v-chip v-if="item.createdAt" size="x-small" color="grey" variant="tonal">
                  {{ item.createdAt?.slice(0, 10) }}
                </v-chip>
              </div>

              <!-- 하단: 상세 정보 -->
              <v-divider class="my-2" />
              <div class="d-flex flex-wrap gap-4 text-caption text-grey-darken-1">
                <span v-if="item.period">
                  <v-icon size="13" class="mr-1">mdi-calendar-range</v-icon>청약기간: {{ item.period }}
                </span>
                <span v-if="item.fixedOfferingPrice">
                  <v-icon size="13" class="mr-1">mdi-currency-krw</v-icon>확정공모가: {{ item.fixedOfferingPrice }}
                </span>
                <span v-if="item.expectedOfferingPrice">
                  희망공모가: {{ item.expectedOfferingPrice }}
                </span>
                <span v-if="item.subscriptionRate">
                  경쟁률: {{ item.subscriptionRate }}
                </span>
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
            <p v-else class="text-caption text-grey py-4">모든 공모주를 불러왔습니다.</p>
          </div>
        </div>

      </v-container>
    </v-main>
  </v-app>
</template>
