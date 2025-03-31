import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  server: {
    port: 3000, // 원하는 포트 번호로 변경
  },
  plugins: [vue()],
  resolve: {
    extensions: ['.mjs', '.js', '.ts', '.json'],
  },
})
