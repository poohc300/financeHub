<script setup lang="ts">
import { RouterView, RouterLink } from 'vue-router'
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100">
    <nav class="bg-white shadow-lg border-b border-gray-200">
      <div class="container mx-auto px-4">
        <div class="flex items-center justify-between h-16">
          <div class="flex items-center">
            <span class="text-xl font-bold text-blue-600">Finance Hub</span>
          </div>
          <div class="flex space-x-2">
            <RouterLink 
              v-for="(item, index) in [
                { to: '/', label: '홈' },
                { to: '/news', label: '뉴스' },
                { to: '/stocks', label: '주식' }
              ]"
              :key="index"
              :to="item.to"
              class="px-4 py-2 rounded-lg transition-colors duration-200 font-medium"
              :class="$route.path === item.to ? 'bg-blue-100 text-blue-700' : 'text-gray-600 hover:bg-gray-100'"
            >
              {{ item.label }}
            </RouterLink>
          </div>
        </div>
      </div>
    </nav>
    
    <main class="container mx-auto px-4 py-8">
      <RouterView v-slot="{ Component }">
        <transition 
          name="fade" 
          mode="out-in"
        >
          <component :is="Component" />
        </transition>
      </RouterView>
    </main>
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