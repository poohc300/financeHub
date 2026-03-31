<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { fetchRequest } from '../util/fetchRequest'

interface Stock52Week {
  isuCd: string
  isuNm: string
  excd: string
  curPrc: number
  high52w: number
  low52w: number
  fromHighPct: number  // 신고가 대비 % (음수 = 고점 아래)
  fromLowPct: number   // 저가 대비 % (양수 = 저점 위)
  rangePct: number     // 52주 범위 내 위치 0~100
}

const stocks = ref<Stock52Week[]>([])
const loading = ref(false)
const exFilter = ref('ALL')

const exFilters = ['ALL', 'NAS', 'NYS']

const filtered = computed(() => {
  if (exFilter.value === 'ALL') return stocks.value
  return stocks.value.filter(s => s.excd === exFilter.value)
})

// 52주 범위 위치에 따른 색상: 낮을수록 매수 고려 구간(녹색), 높을수록 고점(적색)
const rangeBarColor = (pct: number) => {
  if (pct <= 30) return '#22c55e'   // 저점 근처 — 녹색
  if (pct <= 60) return '#f59e0b'   // 중간 — 주황
  return '#ef4444'                   // 고점 근처 — 적색
}

const rangeLabel = (pct: number) => {
  if (pct <= 20) return '저점 근처'
  if (pct <= 40) return '저점~중간'
  if (pct <= 60) return '중간'
  if (pct <= 80) return '중간~고점'
  return '고점 근처'
}

const fmt = (n: number) => n?.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) ?? '-'
const fmtPct = (n: number) => {
  if (n == null) return '-'
  const s = n >= 0 ? `+${n.toFixed(2)}%` : `${n.toFixed(2)}%`
  return s
}

onMounted(async () => {
  loading.value = true
  try {
    stocks.value = await fetchRequest<Stock52Week[]>('/overseas/52week', 'GET') ?? []
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="max-w-5xl mx-auto">

    <!-- 헤더 -->
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-800 flex items-center gap-2">
        <v-icon color="amber-darken-2">mdi-lightbulb</v-icon>
        투자 인사이트
      </h1>
      <p class="text-sm text-gray-500 mt-1">중장기 투자 판단을 위한 데이터 기반 지표</p>
    </div>

    <!-- 52주 신고가/저가 섹션 -->
    <section class="bg-white rounded-2xl shadow-sm border border-gray-100 p-5">
      <div class="flex items-start justify-between mb-1">
        <div>
          <h2 class="text-lg font-bold text-gray-800">📊 52주 신고가/저가 대비 현재가</h2>
          <p class="text-xs text-gray-500 mt-0.5">
            바가 낮을수록 52주 저점에 가까운 구간 — 분할 매수 고려 기준으로 활용
          </p>
        </div>
      </div>

      <!-- 거래소 필터 -->
      <div class="flex gap-2 mt-3 mb-4">
        <button
          v-for="f in exFilters"
          :key="f"
          @click="exFilter = f"
          class="px-3 py-1 rounded-full text-xs font-semibold transition-colors"
          :class="exFilter === f
            ? 'bg-blue-600 text-white'
            : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
        >{{ f }}</button>
      </div>

      <!-- 로딩 -->
      <div v-if="loading" class="flex justify-center py-12">
        <v-progress-circular indeterminate color="primary" />
      </div>

      <!-- 카드 그리드 -->
      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
        <div
          v-for="s in filtered"
          :key="s.isuCd"
          class="border border-gray-100 rounded-xl p-4 hover:shadow-md transition-shadow"
        >
          <!-- 종목 헤더 -->
          <div class="flex items-center justify-between mb-2">
            <div>
              <span class="text-base font-bold text-gray-900">{{ s.isuCd }}</span>
              <span class="ml-1.5 text-xs text-gray-400">{{ s.excd }}</span>
            </div>
            <span
              class="text-xs px-2 py-0.5 rounded-full font-semibold"
              :style="{ backgroundColor: rangeBarColor(s.rangePct) + '22', color: rangeBarColor(s.rangePct) }"
            >{{ rangeLabel(s.rangePct) }}</span>
          </div>
          <p class="text-xs text-gray-400 mb-3 truncate">{{ s.isuNm }}</p>

          <!-- 현재가 -->
          <div class="text-xl font-bold text-gray-900 mb-3">${{ fmt(s.curPrc) }}</div>

          <!-- 52주 범위 바 -->
          <div class="mb-1">
            <div class="h-2 bg-gray-100 rounded-full overflow-hidden">
              <div
                class="h-full rounded-full transition-all duration-500"
                :style="{ width: s.rangePct + '%', backgroundColor: rangeBarColor(s.rangePct) }"
              />
            </div>
            <div class="flex justify-between text-[10px] text-gray-400 mt-0.5">
              <span>저 ${{ fmt(s.low52w) }}</span>
              <span>고 ${{ fmt(s.high52w) }}</span>
            </div>
          </div>

          <!-- 고가/저가 대비 % -->
          <div class="flex justify-between mt-3 text-xs">
            <div>
              <span class="text-gray-400">신고가 대비 </span>
              <span class="font-semibold" :class="s.fromHighPct >= 0 ? 'text-green-600' : 'text-red-500'">
                {{ fmtPct(s.fromHighPct) }}
              </span>
            </div>
            <div>
              <span class="text-gray-400">저가 대비 </span>
              <span class="font-semibold text-blue-600">
                {{ fmtPct(s.fromLowPct) }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 데이터 없음 -->
      <div v-if="!loading && filtered.length === 0" class="text-center py-12 text-gray-400 text-sm">
        데이터가 없습니다. 해외주식 수집 후 다시 시도하세요.
      </div>
    </section>

  </div>
</template>
