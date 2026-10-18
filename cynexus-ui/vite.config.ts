import path from 'node:path'
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import prefixSelector from 'postcss-prefix-selector'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backendUrl = env.VITE_CYNEXUS_BACKEND_URL || 'http://localhost:18000'

  return {
  plugins: [react(), tailwindcss()],
  css: {
    postcss: {
      plugins: [prefixSelector({
        prefix: ':where(.mobile-theme)',
        includeFiles: [/[/\\]mobile[/\\]styles\.css$/],
        transform(prefix, selector, prefixed, _file, rule) {
          // Nested selectors inherit their outer rule's scope.
          for (let parent: typeof rule.parent | NonNullable<typeof rule.parent>['parent'] = rule.parent; parent; parent = parent.parent) {
            if (parent.type === 'rule') return selector
          }
          if (selector === '.mobile-theme') return selector
          if (selector === ':root' || selector === ':host') return prefix
          if (selector === '.dark' || selector === '.light' || selector.startsWith('[data-theme=')) return `${prefix}${selector}`
          return prefixed
        },
      })],
    },
  },
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
