<script setup lang="ts">
import { RouterView, RouterLink } from 'vue-router'

const navItems = [
  { to: '/',       label: '홈',    icon: '🏠' },
  { to: '/news',   label: '뉴스',  icon: '📰' },
  { to: '/stocks', label: '주식',  icon: '📈' },
  { to: '/ipo',    label: '공모주', icon: '🏢' },
]
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

    <!-- 모바일 하단 탭 바 (md 미만) -->
    <nav class="md:hidden fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 z-50">
      <div class="grid grid-cols-4">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="flex flex-col items-center justify-center py-2 gap-0.5 transition-colors"
          :class="$route.path === item.to ? 'text-blue-600' : 'text-gray-400'"
        >
          <span class="text-xl leading-none">{{ item.icon }}</span>
          <span class="text-[11px] font-medium">{{ item.label }}</span>
        </RouterLink>
      </div>
    </nav>

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