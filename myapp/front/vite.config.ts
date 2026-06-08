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
      '/proxy-api': {
        target: 'https://demo.jousing.cn',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/proxy-api/, ''),
      },
    },
  },
})
