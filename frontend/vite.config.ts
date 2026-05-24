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
        configure: (proxy) => {
          // SAM local (Werkzeug) emits both Content-Length and an empty
          // Transfer-Encoding header on large responses, which Node.js HTTP
          // parser rejects as a protocol error. Strip the conflicting header.
          proxy.on('proxyRes', (proxyRes) => {
            if (proxyRes.headers['content-length']) {
              delete proxyRes.headers['transfer-encoding']
            }
          })
        },
      },
    },
  },
})
