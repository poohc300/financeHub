<script setup lang="ts">
import { RouterView, RouterLink, useRouter, useRoute } from 'vue-router'
import { useDisplay } from 'vuetify'

const router = useRouter()
const route = useRoute()
const { mdAndUp } = useDisplay()

const navItems = [
  { to: '/',       label: '홈',    icon: 'mdi-home',                     iconActive: 'mdi-home' },
  { to: '/news',   label: '뉴스',  icon: 'mdi-newspaper-variant-outline', iconActive: 'mdi-newspaper-variant' },
  { to: '/stocks', label: '주식',  icon: 'mdi-chart-line',                iconActive: 'mdi-chart-line' },
  { to: '/ipo',    label: '공모주', icon: 'mdi-briefcase-outline',         iconActive: 'mdi-briefcase' },
]

const navigateTo = (path: string) => {
  if (path && path !== route.path) router.push(path)
}
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100">

    <!-- 데스크탑 상단 네비게이션 (md 이상) -->
    <nav class="hidden md:block bg-white shadow-sm border-b border-gray-200">
      <div class="container mx-auto px-4">
        <div class="flex items-center justify-between h-16">
          <span class="text-xl font-bold text-blue-600">Finance Hub</span>
          <div class="flex space-x-1">
            <RouterLink
              v-for="item in navItems"
              :key="item.to"
              :to="item.to"
              class="px-4 py-2 rounded-lg transition-colors duration-200 font-medium text-sm"
              :class="$route.path === item.to ? 'bg-blue-100 text-blue-700' : 'text-gray-600 hover:bg-gray-100'"
            >
              {{ item.label }}
            </RouterLink>
          </div>
        </div>
      </div>
    </nav>

    <!-- 모바일 상단 헤더 (md 미만) -->
    <header class="md:hidden bg-white border-b border-gray-200 px-4 h-12 flex items-center">
      <span class="text-lg font-bold text-blue-600">Finance Hub</span>
    </header>

    <!-- 본문 — 모바일은 하단 탭바 높이만큼 패딩 추가 -->
    <main class="container mx-auto px-4 py-6 pb-24 md:pb-8">
      <RouterView v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </RouterView>
    </main>

    <!-- 모바일 하단 탭바 — Vuetify v-bottom-navigation (md 미만) -->
    <v-bottom-navigation
      v-if="!mdAndUp"
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

  </div>
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
