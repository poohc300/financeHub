/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  corePlugins: {
    // Vuetify가 자체 CSS reset을 가지고 있으므로 Tailwind preflight 비활성화
    preflight: false,
  },
  theme: {
    extend: {},
  },
  plugins: [],
}
