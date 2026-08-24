import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// The production build output is copied into the app Spring Boot module's
// src/main/resources/static so the app serves API + UI from one origin.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  build: {
    outDir: '../app/src/main/resources/static',
    emptyOutDir: true,
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8090',
      // booking-service / order-service / payment-service controllers are
      // mounted at bare paths (no /api prefix) on the same app process.
      '/bookings': 'http://localhost:8090',
      '/orders': 'http://localhost:8090',
      '/payments': 'http://localhost:8090',
    },
  },
})
