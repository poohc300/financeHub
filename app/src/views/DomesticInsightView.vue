<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { fetchRequest } from '../util/fetchRequest'

interface Stock52Week {
  isuCd: string
  isuSrtCd: string
  isuNm: string
  mktNm: string
  curPrc: number
  high52w: number
  low52w: number
  fromHighPct: number
  fromLowPct: number
  rangePct: number
}

const stocks = ref<Stock52Week[]>([])
const loading = ref(false)
const mktFilter = ref('ALL')
const showTooltip = ref(false)

const mktFilters = ['ALL', 'KOSPI', 'KOSDAQ']

const filtered = computed(() => {
  if (mktFilter.value === 'ALL') return stocks.value
  return stocks.value.filter(s => s.mktNm === mktFilter.value)
})

const rangeBarColor = (pct: number) => {
  if (pct <= 30) return '#22c55e'
  if (pct <= 60) return '#f59e0b'
  return '#ef4444'
}

const rangeLabel = (pct: number) => {
  if (pct <= 20) return '저점 근처'
  if (pct <= 40) return '저점~중간'
  if (pct <= 60) return '중간'
  if (pct <= 80) return '중간~고점'
  return '고점 근처'
}

const fmt = (n: number) => n?.toLocaleString('ko-KR') ?? '-'
const fmtPct = (n: number) => {
  if (n == null) return '-'
  return n >= 0 ? `+${n.toFixed(2)}%` : `${n.toFixed(2)}%`
}

onMounted(async () => {
  loading.value = true
  try {
    stocks.value = await fetchRequest<Stock52Week[]>('/stocks/52week', 'GET') ?? []
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
        <button
          @click="showTooltip = !showTooltip"
          class="ml-1 w-6 h-6 rounded-full bg-gray-200 hover:bg-gray-300 text-gray-500 text-xs font-bold flex items-center justify-center transition-colors"
        >?</button>
      </h1>
      <p class="text-sm text-gray-500 mt-1">중장기 투자 판단을 위한 데이터 기반 지표</p>

      <!-- 설명 패널 -->
      <div v-if="showTooltip" class="mt-3 bg-blue-50 border border-blue-100 rounded-xl p-4 text-sm text-gray-700 space-y-3">
        <p class="font-semibold text-blue-800">📌 이 지표는 어떻게 활용하나요?</p>
        <p>
          주식은 <strong>언제 사느냐</strong>가 수익률을 크게 좌우합니다.
          52주 신고가/저가는 현재 주가가 최근 1년 범위에서 어느 위치인지 보여주는 기준점입니다.
        </p>
        <div>
          <p class="font-semibold mb-1">📊 바(Bar)의 의미</p>
          <p>바의 왼쪽 끝은 <strong>52주 최저가</strong>, 오른쪽 끝은 <strong>52주 최고가</strong>입니다.</p>
          <ul class="mt-1.5 space-y-1 pl-2">
            <li>🟢 <strong>저점 근처 (0~30%)</strong> — 최근 1년 중 바닥 가격대. 분할 매수 고려 구간</li>
            <li>🟡 <strong>중간 (30~60%)</strong> — 고점도 저점도 아닌 구간. 추세 확인 후 판단</li>
            <li>🔴 <strong>고점 근처 (60~100%)</strong> — 최근 1년 고점에 근접. 신규 매수보다 관망 고려</li>
          </ul>
        </div>
        <div>
          <p class="font-semibold mb-1">⚠️ 주의사항</p>
          <p>이 지표는 <strong>단독으로 매수 신호가 아닙니다.</strong> 저점 근처라도 실적 악화·거시 위기 시 더 내려갈 수 있습니다. 현재가는 DB에 수집된 최신 종가 기준이며, 실시간 시세가 아닙니다.</p>
        </div>
      </div>
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

      <!-- 시장 필터 -->
      <div class="flex gap-2 mt-3 mb-4">
        <button
          v-for="f in mktFilters"
          :key="f"
          @click="mktFilter = f"
          class="px-3 py-1 rounded-full text-xs font-semibold transition-colors"
          :class="mktFilter === f
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
              <span class="text-base font-bold text-gray-900">{{ s.isuSrtCd }}</span>
              <span class="ml-1.5 text-xs text-gray-400">{{ s.mktNm }}</span>
            </div>
            <span
              class="text-xs px-2 py-0.5 rounded-full font-semibold"
              :style="{ backgroundColor: rangeBarColor(s.rangePct) + '22', color: rangeBarColor(s.rangePct) }"
            >{{ rangeLabel(s.rangePct) }}</span>
          </div>
          <p class="text-xs text-gray-400 mb-3 truncate">{{ s.isuNm }}</p>

          <!-- 현재가 -->
          <div class="text-xl font-bold text-gray-900 mb-3">₩{{ fmt(s.curPrc) }}</div>

          <!-- 52주 범위 바 -->
          <div class="mb-1">
            <div class="h-2 bg-gray-100 rounded-full overflow-hidden">
              <div
                class="h-full rounded-full transition-all duration-500"
                :style="{ width: s.rangePct + '%', backgroundColor: rangeBarColor(s.rangePct) }"
              />
            </div>
            <div class="flex justify-between text-[10px] text-gray-400 mt-0.5">
              <span>저 ₩{{ fmt(s.low52w) }}</span>
              <span>고 ₩{{ fmt(s.high52w) }}</span>
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
        데이터가 없습니다. 국내주식 수집 후 다시 시도하세요.
      </div>
    </section>

  </div>
</template>
