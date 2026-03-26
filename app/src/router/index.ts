import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import NewsView from '../views/NewsView.vue'
import StockView from '../views/StockView.vue'
import IpoView from '../views/IpoView.vue'
import WatchlistView from '../views/WatchlistView.vue'
import OverseasView from '../views/OverseasView.vue'

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
    },
    {
      path: '/overseas',
      name: 'overseas',
      component: OverseasView
    },
    {
      path: '/ipo',
      name: 'ipo',
      component: IpoView
    },
    {
      path: '/watchlist',
      name: 'watchlist',
      component: WatchlistView
    }
  ]
})

export default router