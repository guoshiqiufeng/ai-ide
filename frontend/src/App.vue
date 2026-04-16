<template>
  <el-container class="app-container">
    <el-aside width="220px" class="app-aside">
      <div class="logo">
        <el-icon :size="24"><Monitor /></el-icon>
        <span>AI IDE</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        class="app-menu"
        background-color="#1d1e1f"
        text-color="#bbb"
        active-text-color="#409eff"
      >
        <el-menu-item index="/chat">
          <el-icon><ChatDotRound /></el-icon>
          <span>Chat</span>
        </el-menu-item>
        <el-menu-item index="/settings">
          <el-icon><Setting /></el-icon>
          <span>API Settings</span>
        </el-menu-item>
        <el-menu-item index="/presets">
          <el-icon><Collection /></el-icon>
          <span>API Presets</span>
        </el-menu-item>
        <el-menu-item index="/prompts">
          <el-icon><Document /></el-icon>
          <span>Prompts</span>
        </el-menu-item>
        <el-menu-item index="/messages">
          <el-icon><Message /></el-icon>
          <span>Messages</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  width: 100%;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}

.app-container {
  height: 100vh;
}

.app-aside {
  background-color: #1d1e1f;
  border-right: 1px solid #333;
  overflow-y: auto;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px;
  color: #fff;
  font-size: 20px;
  font-weight: bold;
  border-bottom: 1px solid #333;
}

.app-menu {
  border-right: none;
}

.app-main {
  background-color: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
}
</style>
