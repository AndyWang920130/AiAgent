import { createApp } from 'vue'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import App from './App.vue'
import router from './router'
import { i18n } from './i18n'

createApp(App).use(router).use(Antd).use(i18n).mount('#app')
