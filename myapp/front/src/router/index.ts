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
        { path: 'tools/json-to-entity', name: 'json-to-entity', component: () => import('../views/JsonToEntityView.vue') },
        { path: 'guide', name: 'user-guide', component: () => import('../views/UserGuideView.vue') },
        { path: 'guide/:slug', name: 'guide-tutorial', component: () => import('../views/GuideTutorialView.vue') },
        { path: 'personal', name: 'personal', component: () => import('../views/PersonalView.vue') },
        { path: 'users/:login', name: 'user-profile', component: () => import('../views/UserProfileView.vue') },
        { path: 'mini-game/lottery', name: 'lottery', component: () => import('../views/LotteryView.vue') },
        { path: 'mini-game/class-lottery', name: 'class-lottery', component: () => import('../views/ClassLotteryView.vue') },
        { path: 'mini-game/gomoku', name: 'gomoku', component: () => import('../views/GomokuView.vue') },
        { path: 'mini-game/card-games', name: 'card-games', component: () => import('../views/CardGameListView.vue') },
        { path: 'mini-game/card-games/70235', name: 'card-game-70235', component: () => import('../views/CardGame70235View.vue') },
        { path: 'blog', name: 'blog-list', component: () => import('../views/BlogListView.vue'), meta: { requiresAdmin: true } },
        { path: 'blog/add', name: 'add-blog', component: () => import('../views/AddBlogView.vue') },
        { path: 'blog/:id', name: 'blog-detail', component: () => import('../views/BlogDetailView.vue') },
        { path: 'blog/:id/edit', name: 'blog-edit', component: () => import('../views/BlogEditView.vue') },
        { path: 'settings/blog-config', name: 'blog-config', component: () => import('../views/BlogConfigView.vue'), meta: { requiresAdmin: true } },
        { path: 'settings/game-config', name: 'game-config', component: () => import('../views/GameConfigView.vue'), meta: { requiresAdmin: true } },
        { path: 'settings/role-config', name: 'role-config', component: () => import('../views/RoleConfigView.vue'), meta: { requiresAdmin: true } },
        { path: 'ecg', name: 'ecg-chart', component: () => import('../views/EcgChartView.vue'), meta: { requiresAdmin: true } },
        { path: 'data-integration', name: 'data-integration', component: () => import('../views/DataIntegrationView.vue'), meta: { requiresAdmin: true } },
        { path: 'data-integration/add', name: 'data-integration-add', component: () => import('../views/DataIntegrationEditView.vue'), meta: { requiresAdmin: true } },
        { path: 'data-integration/:id/edit', name: 'data-integration-edit', component: () => import('../views/DataIntegrationEditView.vue'), meta: { requiresAdmin: true } },
        { path: 'stock/list', name: 'stock-list', component: () => import('../views/StockListView.vue') },
        { path: 'admin/users', name: 'user-list', component: () => import('../views/UserListView.vue'), meta: { requiresAdmin: true } },
        { path: 'notifications', name: 'notifications', component: () => import('../views/NotificationView.vue') },
        { path: 'feedback', name: 'feedback', component: () => import('../views/FeedbackView.vue') },
        { path: 'admin/feedbacks', name: 'feedback-admin', component: () => import('../views/FeedbackAdminView.vue'), meta: { requiresAdmin: true } },
        { path: 'admin/card-game-records', name: 'card-game-records', component: () => import('../views/CardGameRecordView.vue'), meta: { requiresAdmin: true } },
        // No requiresAdmin on either: both are read by everybody. Release Notes shows its
        // editing controls only to an administrator, and the write endpoints it calls sit
        // under /api/v1/admin, which the server restricts.
        { path: 'about', name: 'about', component: () => import('../views/AboutView.vue') },
        { path: 'about/release-notes', name: 'release-notes', component: () => import('../views/ReleaseNotesView.vue') },
      ],
    },
  ],
})

/**
 * The destination to send a user to after they sign in, from the `redirect` query the guard
 * below writes. Returns null for anything that is not a plain in-app path.
 *
 * This value arrives in the URL, so it is attacker-controlled: a link to
 * `/login?redirect=https://elsewhere.example` would otherwise hand the app a one-click
 * open redirect off the back of a real login. Only a path rooted at a single `/` is
 * accepted — `//host` is protocol-relative and `/\host` is read as one by some browsers,
 * so both are refused.
 */
export function resolveLoginRedirect(raw: unknown): string | null {
  if (typeof raw !== 'string' || !raw.startsWith('/')) return null
  if (raw.startsWith('//') || raw.startsWith('/\\')) return null
  // Bouncing back to the login page would just show it again.
  if (raw === '/login' || raw.startsWith('/login?')) return null
  return raw
}

router.beforeEach(to => {
  // fullPath, not path: a shared link keeps its query and hash, and they are part of where
  // the recipient was trying to go.
  if (to.meta.requiresAuth && !isLoggedIn()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && getUser<{ role?: string }>()?.role !== 'ADMIN') return '/home'
})

export default router
