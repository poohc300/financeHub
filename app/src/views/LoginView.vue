<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api/authAPI'
import { useAuth } from '../composables/useAuth'

const router = useRouter()
const { setAuth } = useAuth()

const username = ref('')
const password = ref('')
const errorMsg = ref('')
const loading = ref(false)
const showPassword = ref(false)

async function handleLogin() {
  if (!username.value || !password.value) {
    errorMsg.value = '아이디와 비밀번호를 입력하세요.'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await login(username.value, password.value)
    setAuth(res.accessToken, res.username, res.roles, res.permissions)
    router.push('/')
  } catch (e: unknown) {
    errorMsg.value = e instanceof Error ? e.message : '로그인 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-gray-100">
    <div class="bg-white rounded-2xl shadow-lg p-8 w-full max-w-sm">
      <!-- 로고 -->
      <div class="text-center mb-8">
        <span class="text-2xl font-bold text-blue-600">Finance Hub</span>
        <p class="text-sm text-gray-500 mt-1">로그인</p>
      </div>

      <!-- 폼 -->
      <form @submit.prevent="handleLogin" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">아이디</label>
          <input
            v-model="username"
            type="text"
            autocomplete="username"
            placeholder="아이디 입력"
            class="w-full px-4 py-2.5 rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-400 text-sm"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">비밀번호</label>
          <div class="relative">
            <input
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="current-password"
              placeholder="비밀번호 입력"
              class="w-full px-4 py-2.5 rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-400 text-sm pr-10"
            />
            <button
              type="button"
              @click="showPassword = !showPassword"
              class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
              tabindex="-1"
            >
              <v-icon size="18">{{ showPassword ? 'mdi-eye-off' : 'mdi-eye' }}</v-icon>
            </button>
          </div>
        </div>

        <!-- 에러 메시지 -->
        <p v-if="errorMsg" class="text-red-500 text-xs">{{ errorMsg }}</p>

        <!-- 로그인 버튼 -->
        <button
          type="submit"
          :disabled="loading"
          class="w-full py-2.5 bg-blue-600 text-white rounded-lg font-medium text-sm hover:bg-blue-700 disabled:opacity-50 transition-colors duration-200"
        >
          <span v-if="loading">로그인 중...</span>
          <span v-else>로그인</span>
        </button>
      </form>
    </div>
  </div>
</template>
