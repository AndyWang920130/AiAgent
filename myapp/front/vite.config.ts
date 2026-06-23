import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': resolve(__dirname, 'src') },
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
