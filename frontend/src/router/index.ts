import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/dashboard' },

  {
    path: '/',
    component: () => import('@/layouts/BlankLayout.vue'),
    children: [
      {
        path: 'login',
        name: 'Login',
        component: () => import('@/pages/auth/LoginPage.vue'),
        meta: { public: true, title: '登录' }
      },
      {
        path: 'register',
        name: 'Register',
        component: () => import('@/pages/auth/RegisterPage.vue'),
        meta: { public: true, title: '注册' }
      }
    ]
  },

  {
    path: '/',
    component: () => import('@/layouts/AppLayout.vue'),
    children: [
      {
        path: 'onboarding',
        name: 'Onboarding',
        component: () => import('@/pages/onboarding/OnboardingPage.vue'),
        meta: { title: '建档引导' }
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/pages/dashboard/DashboardPage.vue'),
        meta: { title: '成长总览' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/pages/profile/ProfilePage.vue'),
        meta: { title: '成长档案' }
      },
      {
        path: 'target',
        name: 'Target',
        component: () => import('@/pages/target/TargetPage.vue'),
        meta: { title: '目标设定' }
      },
      {
        path: 'journal',
        name: 'Journal',
        component: () => import('@/pages/journal/JournalListPage.vue'),
        meta: { title: '成长随记' }
      },
      {
        path: 'journal/:id',
        name: 'JournalDetail',
        component: () => import('@/pages/journal/JournalDetailPage.vue'),
        meta: { title: '随记详情' }
      },
      {
        path: 'diagnosis',
        name: 'Diagnosis',
        component: () => import('@/pages/diagnosis/DiagnosisPage.vue'),
        meta: { title: '阶段诊断' }
      },
      {
        path: 'diagnosis/history',
        name: 'DiagnosisHistory',
        component: () => import('@/pages/diagnosis/DiagnosisHistoryPage.vue'),
        meta: { title: '诊断历史' }
      },
      {
        path: 'execution',
        name: 'Execution',
        component: () => import('@/pages/execution/ExecutionPage.vue'),
        meta: { title: '成长执行' }
      }
    ]
  },

  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/pages/NotFoundPage.vue'),
    meta: { public: true, title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  const requiresAuth = !to.meta.public
  if (requiresAuth && !auth.isLoggedIn) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'Login' && auth.isLoggedIn) {
    return { name: 'Dashboard' }
  }
  return true
})

router.afterEach((to) => {
  const title = (to.meta?.title as string | undefined) ?? ''
  document.title = title ? `${title} · GrowthTrace` : 'GrowthTrace'
})

export default router
