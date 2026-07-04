<template>
  <div class="layout" :class="{ 'layout-home': isHome }">
    <div class="layout-overlay" />
    <header class="header-wrap">
      <div class="header-inner">
        <router-link to="/home" class="brand-capsule">
          <span class="logo-icon">◆</span>
          <span class="logo-text">码影拾光</span>
        </router-link>
        <nav class="nav-capsule">
          <router-link to="/home" class="nav-item" active-class="active">Home</router-link>
          <router-link to="/edit" class="nav-item" active-class="active">Edit</router-link>
          <router-link to="/collage" class="nav-item" active-class="active">Collage</router-link>
          <router-link to="/gallery" class="nav-item" active-class="active">Gallery</router-link>
          <router-link to="/profile" class="nav-item" active-class="active">{{ username }} 个人中心</router-link>
        </nav>
      </div>
    </header>
    <main class="main" :class="{ 'main-home': isHome }">
      <router-view />
    </main>
    <MusicPlayer />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import MusicPlayer from '../components/MusicPlayer.vue'

const route = useRoute()
const username = computed(() => localStorage.getItem('username') || 'user')
const isHome = computed(() => route.path === '/home')
</script>

<style scoped>
.layout {
  position: relative;
  min-height: 100vh;
  background: url('/app-bg.png') center / cover no-repeat fixed;
}

.layout-home {
  background: #0f1419;
}

.layout-home .layout-overlay {
  display: none;
}

.layout-overlay {
  position: fixed;
  inset: 0;
  background: rgba(245, 247, 250, 0);
  pointer-events: none;
  z-index: 0;
}

.header-wrap {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  padding: 14px 20px;
  pointer-events: none;
}

.header-inner {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  pointer-events: auto;
}

.brand-capsule {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: 999px;
  background: rgba(18, 18, 28, 0.72);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.35),
    0 0 0 1px rgba(255, 255, 255, 0.04) inset;
  text-decoration: none;
  flex-shrink: 0;
  transition: background 0.2s, box-shadow 0.2s;
}

.brand-capsule:hover {
  background: rgba(28, 28, 42, 0.82);
  box-shadow:
    0 6px 28px rgba(0, 0, 0, 0.4),
    0 0 20px rgba(100, 120, 255, 0.12);
}

.logo-icon {
  color: #6eb5ff;
  font-size: 18px;
  line-height: 1;
}

.logo-text {
  font-size: 16px;
  font-style: italic;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.92);
  letter-spacing: 0.5px;
  white-space: nowrap;
}

.nav-capsule {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 6px;
  border-radius: 999px;
  background: rgba(18, 18, 28, 0.72);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.35),
    0 0 0 1px rgba(255, 255, 255, 0.04) inset;
  flex-shrink: 1;
  min-width: 0;
}

.nav-item {
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.62);
  text-decoration: none;
  white-space: nowrap;
  transition: color 0.2s, background 0.2s;
  line-height: 1.2;
}

.nav-item:hover {
  color: rgba(255, 255, 255, 0.9);
}

.nav-item.active {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  font-weight: 500;
  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.25),
    0 0 0 1px rgba(255, 255, 255, 0.06) inset;
}

.main {
  position: relative;
  z-index: 1;
  padding: 80px 48px 32px;
}

.main-home {
  padding: 0;
}

@media (max-width: 768px) {
  .header-wrap {
    padding: 10px 12px;
  }

  .header-inner {
    gap: 8px;
  }

  .brand-capsule {
    padding: 8px 14px;
  }

  .logo-text {
    font-size: 14px;
  }

  .nav-capsule {
    gap: 2px;
    padding: 4px;
    overflow-x: auto;
    max-width: calc(100vw - 140px);
    scrollbar-width: none;
  }

  .nav-capsule::-webkit-scrollbar {
    display: none;
  }

  .nav-item {
    padding: 7px 10px;
    font-size: 12px;
  }

  .main {
    padding: 72px 16px 24px;
  }
}
</style>
