import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn } from '../utils/auth'

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
        { path: 'personal', name: 'personal', component: () => import('../views/PersonalView.vue') },
        { path: 'blog', name: 'blog-list', component: () => import('../views/BlogListView.vue') },
        { path: 'blog/add', name: 'add-blog', component: () => import('../views/AddBlogView.vue') },
        { path: 'blog/:id', name: 'blog-detail', component: () => import('../views/BlogDetailView.vue') },
        { path: 'blog/:id/edit', name: 'blog-edit', component: () => import('../views/BlogEditView.vue') },
      ],
    },
  ],
})

router.beforeEach(to => {
  if (to.meta.requiresAuth && !isLoggedIn()) return '/login'
})

export default router
