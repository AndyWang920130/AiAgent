import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn, getUser } from '../utils/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
    { path: '/register', name: 'register', component: () => import('../views/RegisterView.vue') },
    { path: '/forgot-password', name: 'forgot', component: () => import('../views/ForgotPasswordView.vue') },
    {
      path: '/',
      component: () => import('../layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/home' },
        { path: 'home', name: 'home', component: () => import('../views/HomeView.vue') },
        { path: 'tools', name: 'tools', component: () => import('../views/ToolsView.vue') },
        { path: 'tools/aes', name: 'aes-tool', component: () => import('../views/AesToolView.vue') },
        { path: 'tools/sha', name: 'sha-tool', component: () => import('../views/ShaToolView.vue') },
        { path: 'personal', name: 'personal', component: () => import('../views/PersonalView.vue') },
        { path: 'mini-game/lottery', name: 'lottery', component: () => import('../views/LotteryView.vue') },
        { path: 'mini-game/class-lottery', name: 'class-lottery', component: () => import('../views/ClassLotteryView.vue') },
        { path: 'blog', name: 'blog-list', component: () => import('../views/BlogListView.vue') },
        { path: 'blog/add', name: 'add-blog', component: () => import('../views/AddBlogView.vue') },
        { path: 'blog/:id', name: 'blog-detail', component: () => import('../views/BlogDetailView.vue') },
        { path: 'blog/:id/edit', name: 'blog-edit', component: () => import('../views/BlogEditView.vue') },
        { path: 'settings/blog-config', name: 'blog-config', component: () => import('../views/BlogConfigView.vue'), meta: { requiresAdmin: true } },
        { path: 'settings/game-config', name: 'game-config', component: () => import('../views/GameConfigView.vue'), meta: { requiresAdmin: true } },
        { path: 'ecg', name: 'ecg-chart', component: () => import('../views/EcgChartView.vue'), meta: { requiresAdmin: true } },
        { path: 'notifications', name: 'notifications', component: () => import('../views/NotificationView.vue') },
      ],
    },
  ],
})

router.beforeEach(to => {
  if (to.meta.requiresAuth && !isLoggedIn()) return '/login'
  if (to.meta.requiresAdmin && getUser<{ role?: string }>()?.role !== 'ADMIN') return '/home'
})

export default router
