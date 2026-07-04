<template>
  <div class="login-page">
    <div class="login-overlay" />
    <div class="login-card">
      <div class="brand-block">
        <div class="brand">
          <span class="brand-icon">◆</span>
          <h1 class="title">码影拾光</h1>
        </div>
        <p class="subtitle">智能照片整理应用</p>
      </div>

      <el-tabs v-model="tab" class="login-tabs">
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" @submit.prevent="handleLogin">
            <el-form-item>
              <el-input v-model="loginForm.username" placeholder="用户名" prefix-icon="User" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="loginForm.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
            </el-form-item>
            <el-button type="primary" class="submit-btn" :loading="loading" @click="handleLogin">
              登录
            </el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" @submit.prevent="handleRegister">
            <el-form-item>
              <el-input v-model="registerForm.username" placeholder="用户名（3-20位）" prefix-icon="User" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="registerForm.password" type="password" placeholder="密码（6位以上）" prefix-icon="Lock" show-password />
            </el-form-item>
            <el-button type="primary" class="submit-btn" :loading="loading" @click="handleRegister">
              注册
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '../api/auth'

const router = useRouter()
const tab = ref('login')
const loading = ref(false)

const loginForm = ref({ username: '', password: '' })
const registerForm = ref({ username: '', password: '' })

async function handleLogin() {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await login(loginForm.value)
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('username', res.data.username)
    ElMessage.success('登录成功')
    router.push('/home')
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!registerForm.value.username || !registerForm.value.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await register(registerForm.value)
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('username', res.data.username)
    ElMessage.success('注册成功')
    router.push('/home')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 80px;
  background: url('/login-bg.png') center / cover no-repeat fixed;
}

.login-overlay {
  position: absolute;
  inset: 0;
  background: transparent;
  pointer-events: none;
}

.login-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  padding: 36px 32px 28px;
  border-radius: 20px;
  animation: card-in 0.6s ease-out;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.85);
  box-shadow:
    0 20px 50px rgba(0, 0, 0, 0.15),
    0 0 0 1px rgba(255, 255, 255, 0.5) inset;
}

@keyframes card-in {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.brand-block {
  margin-bottom: 24px;
}

.brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.brand-icon {
  color: #4a90e2;
  font-size: 28px;
  line-height: 1;
}

.title {
  font-size: 30px;
  font-weight: 700;
  letter-spacing: 1px;
  margin: 0;
  color: #1e293b;
}

.subtitle {
  text-align: center;
  margin: 10px 0 0;
  font-size: 14px;
  letter-spacing: 1px;
  color: #334155;
  font-weight: 600;
}

.login-tabs :deep(.el-tabs__item) {
  font-size: 15px;
}

.login-tabs :deep(.el-tabs__item.is-active) {
  color: #4a90e2;
  font-weight: 600;
}

.login-tabs :deep(.el-tabs__active-bar) {
  background-color: #4a90e2;
}

.login-tabs :deep(.el-input__wrapper) {
  border-radius: 999px;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.08) inset;
  background: rgba(255, 255, 255, 0.85);
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  height: 42px;
  border-radius: 999px;
  font-size: 15px;
  letter-spacing: 2px;
  font-weight: 600;
}

@media (max-width: 768px) {
  .login-page {
    justify-content: center;
    padding: 24px 16px;
  }

  .login-card {
    max-width: 100%;
  }
}
</style>
