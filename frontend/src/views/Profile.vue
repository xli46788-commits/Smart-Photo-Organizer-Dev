<template>
  <div class="profile-page">
    <div class="page-header-capsule">
      <h1>hello, {{ profile.username || 'user' }}</h1>
    </div>

    <div class="info-card panel-capsule">
      <div class="info-row">
        <span class="info-label">用户 ID</span>
        <span class="info-value">{{ profile.userId ?? '-' }}</span>
      </div>
      <div class="info-row">
        <span class="info-label">用户名</span>
        <span class="info-value">{{ profile.username ?? '-' }}</span>
      </div>
      <div class="info-row">
        <span class="info-label">照片数量</span>
        <span class="info-value">{{ profile.photoCount ?? 0 }} 张</span>
      </div>

      <el-button type="danger" class="logout-btn" @click="handleLogout">退出登录</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getProfile } from '../api/auth'

const router = useRouter()
const profile = ref({})

onMounted(async () => {
  const res = await getProfile()
  profile.value = res.data
})

function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  sessionStorage.removeItem('musicSessionAutoplayOff')
  router.push('/login')
}
</script>

<style scoped>
.profile-page {
  max-width: 520px;
  margin: 0 auto;
  padding-bottom: 32px;
}

.page-header-capsule {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: fit-content;
  margin: 0 auto 28px;
  padding: 14px 32px;
  border-radius: 999px;
  background: rgba(18, 18, 28, 0.78);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.3);
}

.page-header-capsule h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 1px;
}

.panel-capsule {
  padding: 24px 28px;
  border-radius: 20px;
  background: rgba(18, 18, 28, 0.78);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.3);
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 16px;
  margin-bottom: 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.info-label {
  font-size: 14px;
  font-weight: 600;
  color: rgba(168, 216, 234, 0.95);
  white-space: nowrap;
}

.info-value {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  text-align: right;
}

.logout-btn {
  width: 100%;
  margin-top: 20px;
  height: 42px;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 600;
  border: none;
  box-shadow: 0 4px 16px rgba(245, 108, 108, 0.35);
}
</style>
