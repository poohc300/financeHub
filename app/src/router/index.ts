import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import { refreshToken } from '../api/authAPI'

import DashboardView from '../views/DashboardView.vue'
import NewsView from '../views/NewsView.vue'
import StockView from '../views/StockView.vue'
import IpoView from '../views/IpoView.vue'
import WatchlistView from '../views/WatchlistView.vue'
import OverseasView from '../views/OverseasView.vue'
import InsightView from '../views/InsightView.vue'
import LoginView from '../views/LoginView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
    { path: '/',          name: 'home',      component: DashboardView },
    { path: '/news',      name: 'news',      component: NewsView },
    { path: '/stocks',    name: 'stocks',    component: StockView },
    { path: '/overseas',  name: 'overseas',  component: OverseasView },
    { path: '/ipo',       name: 'ipo',       component: IpoView },
    { path: '/watchlist', name: 'watchlist', component: WatchlistView },
    { path: '/insight',   name: 'insight',   component: InsightView },
  ],
})

router.beforeEach(async (to) => {
  if (to.meta.public) return true

  const { auth, setAuth, clearAuth } = useAuth()

  // 이미 인증된 경우 통과
  if (auth.accessToken) return true

  // Access Token 없으면 Refresh Token으로 복구 시도
  try {
    const res = await refreshToken()
    setAuth(res.accessToken, res.username, res.roles, res.permissions)
    return true
  } catch {
    clearAuth()
    return { name: 'login', query: { redirect: to.fullPath } }
  }
})

export default router
