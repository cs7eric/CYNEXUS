import path from 'node:path'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    host: '0.0.0.0',
    allowedHosts: ['.trycloudflare.com', 'nooops.makeup', '.nooops.makeup'],
    proxy: {
      '/api/ai': {
        target: 'http://localhost:18000',
        changeOrigin: true,
      },
      '/api/market': {
        target: 'http://localhost:18000',
        changeOrigin: true,
      },
      '/api/core': {
        target: 'http://localhost:18000',
        changeOrigin: true,
      },
      '/api/knowledge-base': {
        target: 'http://localhost:18000',
        changeOrigin: true,
      },
    },
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
})
