<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { fetchRequest } from '../util/fetchRequest'

interface WatchlistItem {
  isuSrtCd: string
  isuCd: string
  isuNm: string
}

interface StockPrice {
  currentPrice: string
  change: string
  changeRate: string
  isRealtime: boolean
}

const STORAGE_KEY = 'watchlist'
const MAX_ITEMS = 5

const router = useRouter()
const watchlist = ref<WatchlistItem[]>([])
const prices = ref<Record<string, StockPrice>>({})
const loading = ref(false)
let ws: WebSocket | null = null

const loadWatchlist = () => {
  try {
    watchlist.value = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
  } catch {
    watchlist.value = []
  }
}

const removeItem = (isuSrtCd: string) => {
  watchlist.value = watchlist.value.filter(w => w.isuSrtCd !== isuSrtCd)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(watchlist.value))
}

const goToChart = (item: WatchlistItem) => {
  router.push({
    path: '/stocks',
    query: { isuSrtCd: item.isuSrtCd, isuCd: item.isuCd, isuNm: item.isuNm }
  })
}

const fetchPrices = async () => {
  if (watchlist.value.length === 0) return
  loading.value = true
  await Promise.all(watchlist.value.map(async (item) => {
    try {
      const data = await fetchRequest<any>(
        `/dashboard/chart-data?market=STOCK&isuCd=${encodeURIComponent(item.isuCd)}&limit=2`, 'GET'
      )
      const vals: string[] = data.values || []
      if (vals.length >= 1) {
        const parse = (v: string) => parseFloat(v?.replace(/,/g, '') || '0')
        const curr = parse(vals[vals.length - 1])
        const prev = vals.length >= 2 ? parse(vals[vals.length - 2]) : curr
        const diff = curr - prev
        const rate = prev !== 0 ? (diff / prev) * 100 : 0
        prices.value[item.isuSrtCd] = {
          currentPrice: curr.toLocaleString(),
          change: diff >= 0 ? '+' + diff.toFixed(0) : diff.toFixed(0),
          changeRate: (rate >= 0 ? '+' : '') + rate.toFixed(2),
          isRealtime: false,
        }
      }
    } catch {}
  }))
  loading.value = false
}

const connectWS = () => {
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  ws = new WebSocket(`${protocol}//${location.host}/ws/stock`)
  ws.onmessage = (event) => {
    try {
      const msg = JSON.parse(event.data)
      if (msg.isuSrtCd && watchlist.value.some(w => w.isuSrtCd === msg.isuSrtCd)) {
        const rate = parseFloat(msg.changeRate)
        prices.value[msg.isuSrtCd] = {
          currentPrice: Number(msg.currentPrice).toLocaleString(),
          change: msg.change,
          changeRate: (rate >= 0 ? '+' : '') + rate.toFixed(2),
          isRealtime: true,
        }
      }
    } catch {}
  }
}

onMounted(async () => {
  loadWatchlist()
  await fetchPrices()
  connectWS()
})

onBeforeUnmount(() => ws?.close())
</script>

<template>
  <div class="max-w-2xl mx-auto">
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-gray-800">관심 종목</h1>
      <span class="text-sm text-gray-400 bg-white border border-gray-200 rounded-full px-3 py-0.5">
        {{ watchlist.length }} / {{ MAX_ITEMS }}
      </span>
    </div>

    <!-- 빈 상태 -->
    <div v-if="watchlist.length === 0" class="flex flex-col items-center justify-center py-24 text-gray-400">
      <v-icon size="52" class="mb-4 opacity-20">mdi-star-outline</v-icon>
      <p class="text-base font-medium text-gray-500">관심 종목이 없습니다</p>
      <p class="text-sm mt-1">주식 탭에서 ★ 버튼으로 추가하세요</p>
      <v-btn class="mt-6" variant="tonal" color="primary" @click="router.push('/stocks')">
        주식 탭으로 이동
      </v-btn>
    </div>

    <!-- 종목 목록 + 빈 슬롯 -->
    <div v-else class="flex flex-col gap-3">
      <!-- 등록된 종목 -->
      <div
        v-for="item in watchlist"
        :key="item.isuSrtCd"
        class="bg-white rounded-xl shadow-sm border border-gray-200 p-4 flex items-center gap-3 hover:shadow-md transition-shadow"
      >
        <v-icon color="amber-darken-1" size="22">mdi-star</v-icon>

        <div class="flex-1 min-w-0 cursor-pointer" @click="goToChart(item)">
          <div class="flex items-center gap-2 flex-wrap">
            <span class="font-bold text-gray-800">{{ item.isuNm }}</span>
            <span class="text-xs text-gray-400">{{ item.isuSrtCd }}</span>
            <span
              v-if="prices[item.isuSrtCd]?.isRealtime"
              class="text-xs bg-green-100 text-green-600 px-1.5 py-0.5 rounded-full font-medium"
            >LIVE</span>
          </div>
          <div v-if="prices[item.isuSrtCd]" class="flex items-baseline gap-2 mt-1">
            <span class="text-lg font-bold text-gray-900">{{ prices[item.isuSrtCd].currentPrice }}원</span>
            <span
              :class="prices[item.isuSrtCd].changeRate.startsWith('-') ? 'text-red-500' : 'text-green-600'"
              class="text-sm font-medium"
            >
              {{ prices[item.isuSrtCd].changeRate.startsWith('-') ? '▼' : '▲' }}
              {{ prices[item.isuSrtCd].changeRate.replace(/^[+-]/, '') }}%
            </span>
          </div>
          <div v-else-if="loading" class="text-xs text-gray-300 mt-1 animate-pulse">불러오는 중...</div>
          <div v-else class="text-xs text-gray-300 mt-1">데이터 없음</div>
        </div>

        <div class="flex items-center gap-1 flex-shrink-0">
          <button
            @click="goToChart(item)"
            class="text-xs text-blue-500 hover:text-blue-700 px-2 py-1.5 rounded-lg hover:bg-blue-50 font-medium transition-colors"
          >
            차트 →
          </button>
          <button
            @click="removeItem(item.isuSrtCd)"
            class="text-xs text-gray-300 hover:text-red-400 px-2 py-1.5 rounded-lg hover:bg-red-50 transition-colors"
          >
            삭제
          </button>
        </div>
      </div>

      <!-- 빈 슬롯 -->
      <div
        v-for="i in MAX_ITEMS - watchlist.length"
        :key="'empty-' + i"
        class="bg-gray-50 border border-dashed border-gray-200 rounded-xl p-4 flex items-center gap-3 cursor-pointer hover:bg-gray-100 transition-colors"
        @click="router.push('/stocks')"
      >
        <v-icon size="22" class="opacity-20">mdi-star-outline</v-icon>
        <span class="text-sm text-gray-300">주식 탭에서 종목을 추가하세요</span>
      </div>
    </div>
  </div>
</template>
