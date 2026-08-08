import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// The production build output is copied into the flight-search Spring Boot
// module's src/main/resources/static so the app serves API + UI from one origin.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  build: {
    outDir: '../flight-search/src/main/resources/static',
    emptyOutDir: true,
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8090',
    },
  },
})
