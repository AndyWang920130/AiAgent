import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath } from 'node:url'
import { resolve } from 'node:path'

const root = fileURLToPath(new URL('.', import.meta.url))

export default defineConfig({
  root,
  plugins: [vue()],
  resolve: {
    alias: { '@': resolve(root, 'src') },
  },
  css: {
    preprocessorOptions: {
      less: { javascriptEnabled: true },
    },
  },
  server: {
    proxy: {
      '/dev/': {
        target: 'http://localhost:8080/',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/dev/, ''),
      },
      '/test/': {
        target: 'https://aaa.com/',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/test/, ''),
      },
    },
  },
})


