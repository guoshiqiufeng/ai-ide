import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/chat'
    },
    {
      path: '/chat',
      name: 'Chat',
      component: () => import('@/views/ChatView.vue')
    },
    {
      path: '/settings',
      name: 'Settings',
      component: () => import('@/views/SettingsView.vue')
    },
    {
      path: '/presets',
      name: 'Presets',
      component: () => import('@/views/PresetsView.vue')
    },
    {
      path: '/prompts',
      name: 'Prompts',
      component: () => import('@/views/PromptsView.vue')
    },
    {
      path: '/messages',
      name: 'Messages',
      component: () => import('@/views/MessagesView.vue')
    }
  ]
})

export default router
