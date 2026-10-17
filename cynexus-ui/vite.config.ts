import path from 'node:path'
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backendUrl = env.VITE_CYNEXUS_BACKEND_URL || 'http://localhost:18000'

  return {
  plugins: [react(), tailwindcss()],
  server: {
    host: '0.0.0.0',
    port: 5174,
    allowedHosts: ['.trycloudflare.com', 'nooops.makeup', '.nooops.makeup'],
    proxy: {
      '/api': {
        target: backendUrl,
        changeOrigin: false,
      },
      '/data-api': {
        target: backendUrl,
        changeOrigin: false,
      },
    },
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      // Recharts 3.8 imports this CommonJS compat entry from Radar; Vite 8's
      // production minifier emits a self-referential initializer for it.
      'es-toolkit/compat/last': path.resolve(__dirname, './src/lib/recharts-compat-last.ts'),
    },
  },
  }
})
