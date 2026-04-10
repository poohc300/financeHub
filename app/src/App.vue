<script setup lang="ts">
import { RouterView, RouterLink, useRouter, useRoute } from 'vue-router'
import { useDisplay } from 'vuetify'
import { useAuth } from './composables/useAuth'
import { logout } from './api/authAPI'

const router = useRouter()
const route = useRoute()
const { mdAndUp } = useDisplay()
const { auth, clearAuth } = useAuth()

const navItems = [
  { to: '/',          label: '홈',     icon: 'mdi-home',                     iconActive: 'mdi-home' },
  { to: '/news',      label: '뉴스',   icon: 'mdi-newspaper-variant-outline', iconActive: 'mdi-newspaper-variant' },
  { to: '/stocks',    label: '주식',   icon: 'mdi-chart-line',                iconActive: 'mdi-chart-line' },
  { to: '/watchlist', label: '관심',   icon: 'mdi-star-outline',              iconActive: 'mdi-star' },
  { to: '/ipo',       label: '공모주', icon: 'mdi-briefcase-outline',         iconActive: 'mdi-briefcase' },
]

const navigateTo = (path: string) => {
  if (path && path !== route.path) router.push(path)
}

async function handleLogout() {
  await logout()
  clearAuth()
  router.push('/login')
}
</script>

<template>
  <v-app style="background: linear-gradient(to bottom right, #f9fafb, #f3f4f6); overflow-y: auto;">

    <!-- 로그인 페이지: 네비바 없이 RouterView만 렌더링 -->
    <template v-if="$route.name === 'login'">
      <RouterView />
    </template>

    <template v-else>

    <!-- 데스크탑 상단 네비게이션 (md 이상) -->
    <nav class="hidden md:block bg-white shadow-sm border-b border-gray-200 sticky top-0 z-50">
      <div class="container mx-auto px-4">
        <div class="flex items-center justify-between h-16">
          <div class="flex items-center gap-3">
            <span class="text-xl font-bold text-blue-600">Finance Hub</span>
            <!-- 해외주식 대시보드 이동 버튼 -->
            <RouterLink
              to="/overseas"
              class="flex items-center gap-1 px-3 py-1.5 rounded-lg text-sm font-semibold transition-colors duration-200"
              :class="$route.path === '/overseas'
                ? 'bg-blue-600 text-white'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
            >
              <v-icon size="15">mdi-earth</v-icon>
              해외주식
            </RouterLink>
          </div>
          <!-- 해외주식 페이지: 국내로 돌아가기 버튼 -->
          <RouterLink
            v-if="$route.path === '/overseas'"
            to="/"
            class="flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-semibold text-gray-600 hover:bg-gray-100 transition-colors duration-200"
          >
            <v-icon size="16">mdi-arrow-left</v-icon>
            국내주식
          </RouterLink>

          <!-- 기존 국내 네비게이션 + 사용자 정보 -->
          <div v-else class="flex items-center gap-2">
            <RouterLink
              v-for="item in navItems"
              :key="item.to"
              :to="item.to"
              class="px-4 py-2 rounded-lg transition-colors duration-200 font-medium text-sm"
              :class="$route.path === item.to ? 'bg-blue-100 text-blue-700' : 'text-gray-600 hover:bg-gray-100'"
            >
              {{ item.label }}
            </RouterLink>
            <div class="ml-4 flex items-center gap-2 border-l border-gray-200 pl-4">
              <span class="text-sm text-gray-600 font-medium">{{ auth.username }}</span>
              <button
                @click="handleLogout"
                class="flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-semibold text-gray-500 hover:bg-gray-100 transition-colors"
              >
                <v-icon size="14">mdi-logout</v-icon>
                로그아웃
              </button>
            </div>
          </div>
        </div>
      </div>
    </nav>

    <!-- 모바일 상단 헤더 (md 미만) -->
    <header class="md:hidden bg-white border-b border-gray-200 px-4 h-12 flex items-center justify-between sticky top-0 z-50">
      <!-- 해외주식 페이지: 국내로 돌아가기 -->
      <template v-if="$route.path === '/overseas'">
        <RouterLink to="/" class="flex items-center gap-1 text-sm font-semibold text-gray-600">
          <v-icon size="16">mdi-arrow-left</v-icon>
          국내주식
        </RouterLink>
        <span class="text-base font-bold text-blue-600 flex items-center gap-1">
          <v-icon size="15">mdi-earth</v-icon>해외주식
        </span>
      </template>
      <!-- 국내 페이지: 로고 + 해외주식 이동 버튼 -->
      <template v-else>
        <span class="text-lg font-bold text-blue-600">Finance Hub</span>
        <RouterLink
          to="/overseas"
          class="flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold bg-gray-100 text-gray-600"
        >
          <v-icon size="13">mdi-earth</v-icon>
          해외주식
        </RouterLink>
      </template>
    </header>

    <!-- 본문 -->
    <main class="container mx-auto px-4 py-6 pb-24 md:pb-8">
      <RouterView v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </RouterView>
    </main>

    <!-- 모바일 하단 탭바 (해외주식 페이지에서는 숨김) -->
    <v-bottom-navigation
      v-if="!mdAndUp && $route.path !== '/overseas'"
      :model-value="route.path"
      @update:model-value="navigateTo"
      grow
      elevation="8"
      color="primary"
      height="60"
    >
      <v-btn
        v-for="item in navItems"
        :key="item.to"
        :value="item.to"
      >
        <v-icon size="22">
          {{ route.path === item.to ? item.iconActive : item.icon }}
        </v-icon>
        <span class="text-[11px] mt-0.5">{{ item.label }}</span>
      </v-btn>
    </v-bottom-navigation>

    </template><!-- end v-else (not login page) -->

  </v-app>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

<style>
html,
body,
.v-application {
  overflow-y: auto !important;
}
</style>
