import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        // 8080 = mvn quarkus:dev (recommended for local development, no Docker needed)
        // 3000 = sam local start-api (requires Docker + `sam build` first)
        target: process.env.VITE_BACKEND_URL ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
