import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import NewsView from '../views/NewsView.vue'
import StockView from '../views/StockView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
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