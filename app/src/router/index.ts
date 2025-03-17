import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import NewsView from '../views/NewsView.vue'
import StockView from '../views/StockView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: DashboardView
    },
    {
      path: '/news',
      name: 'news',
      component: NewsView
    },
    {
      path: '/stocks',
      name: 'stocks',
      component: StockView
    }
  ]
})

export default router