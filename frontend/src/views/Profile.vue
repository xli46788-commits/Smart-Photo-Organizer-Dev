<template>
  <div class="profile-dashboard">
    <div class="page-title-box">
      <h1>个人中心</h1>
    </div>

    <div class="dashboard-grid">
      
      <div class="glass-card user-card">
        <div class="user-info-top">
          <div class="avatar-ring">
            <div class="avatar-inner">
              {{ profile.username?.charAt(0)?.toUpperCase() || 'U' }}
            </div>
          </div>
          <h2 class="greeting">Hello, <span>{{ profile.username || 'User' }}</span></h2>
          <div class="user-id-tag">UID: {{ profile.userId ?? '---' }}</div>
        </div>

        <div class="stats-divider"></div>

        <div class="stats-container">
          <div class="stat-box">
            <el-icon class="stat-icon"><Picture /></el-icon>
            <div class="stat-value">{{ profile.photoCount ?? 0 }}</div>
            <div class="stat-label">留存的印记 (张)</div>
          </div>
        </div>

        <div class="stats-divider"></div>

        <div class="user-actions">
          <el-button type="danger" plain class="logout-btn action-btn-capsule" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon> 退出登录
          </el-button>
        </div>
      </div>

      <div class="glass-card blind-box-card">
        <template v-if="blindBoxPhoto">
          <img :src="blindBoxPhoto.url || blindBoxPhoto.thumbUrl" alt="时光盲盒" class="bb-image" />
          
          <div class="bb-header-tag">
            ✨ 时光盲盒
          </div>

          <div class="bb-overlay">
            <el-button circle size="large" type="primary" class="roll-btn" @click="rollBlindBox" title="再抽一次">
              <el-icon size="28"><RefreshRight /></el-icon>
            </el-button>
          </div>
          
          <div class="bb-info-layer">
            <div class="bb-text-content">
              <p class="bb-quote">" {{ blindBoxText }} "</p>
              <div class="bb-meta">
                <el-tag size="small" effect="dark" round class="bb-category" v-if="blindBoxPhoto.aiCategory">
                  {{ blindBoxPhoto.aiCategory }}
                </el-tag>
                <span class="bb-date" v-if="blindBoxPhoto.createdAt">
                  {{ formatDateTime(blindBoxPhoto.createdAt) }}
                </span>
              </div>
            </div>
          </div>
        </template>
        
        <div class="empty-box" v-else>
          <div class="bb-header-tag">✨ 时光盲盒</div>
          <el-icon size="64"><Box /></el-icon>
          <p>时光机里空空如也<br>快去画廊上传第一张照片，开启盲盒吧！</p>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Picture, SwitchButton, RefreshRight, Box } from '@element-plus/icons-vue'
import { getProfile } from '../api/auth'
import { getPhotos } from '../api/photo'

const router = useRouter()
const profile = ref({})

const blindBoxPhoto = ref(null)
const blindBoxText = ref('')
const photoPool = ref([]) 

const BLIND_BOX_TEXTS = [
  "时光机运转中... 发现了一张被岁月温柔收藏的照片。",
  "今天也是美好的一天，重温一下这张照片吧。",
  "随机掉落的记忆碎片，看看这是哪个瞬间？",
  "有些美好值得反复播放，比如这一刻。",
  "一张老照片，一个好故事，送给现在的你。",
  "时间走得太快，幸好我们按下了快门。",
  "在无数个平凡的日子里，这是闪闪发光的一天。"
]

onMounted(async () => {
  const res = await getProfile()
  profile.value = res.data
  loadBlindBox()
})

async function loadBlindBox() {
  try {
    if (photoPool.value.length === 0) {
      const res = await getPhotos({ page: 1, pageSize: 100 })
      photoPool.value = res.data.records || []
    }
    rollBlindBox()
  } catch (e) {
    console.error('盲盒照片加载失败', e)
  }
}

function rollBlindBox() {
  if (photoPool.value.length > 0) {
    const randomPhotoIndex = Math.floor(Math.random() * photoPool.value.length)
    const randomTextIndex = Math.floor(Math.random() * BLIND_BOX_TEXTS.length)
    
    blindBoxPhoto.value = photoPool.value[randomPhotoIndex]
    blindBoxText.value = BLIND_BOX_TEXTS[randomTextIndex]
  }
}

function formatDateTime(val) {
  if (!val) return ''
  return String(val).includes('T') ? String(val).replace('T', ' ').split('.')[0] : String(val).split('.')[0]
}

function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  sessionStorage.removeItem('musicSessionAutoplayOff')
  router.push('/login')
}
</script>

<style scoped>
/* 全局页面容器 */
.profile-dashboard {
  max-width: 1100px;
  margin: 0 auto;
  padding: 40px 20px 80px;
  min-height: calc(100vh - 100px);
}

.page-title-box {
  margin-bottom: 32px;
  text-align: center;
}

.page-title-box h1 {
  font-size: 32px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: 2px;
  text-shadow: 0 2px 10px rgba(255, 255, 255, 0.8);
}

/* 左右双栏对称网格布局 */
.dashboard-grid {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 32px;
  align-items: stretch; /* 核心：让两边等高 */
}

/* 统一的高级毛玻璃面板 */
.glass-card {
  background: rgba(18, 18, 28, 0.75);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.25);
  overflow: hidden;
  height: 520px; /* 固定统一高度，消灭突兀感 */
  display: flex;
  flex-direction: column;
}

/* ================= 左侧：个人卡片 ================= */
.user-card {
  padding: 48px 32px 32px;
  text-align: center;
  justify-content: space-between;
  position: relative;
}

.user-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 6px;
  background: linear-gradient(90deg, #4facfe 0%, #00f2fe 100%);
}

.avatar-ring {
  width: 100px;
  height: 100px;
  margin: 0 auto 20px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  padding: 4px;
  box-shadow: 0 8px 24px rgba(79, 172, 254, 0.3);
}

.avatar-inner {
  width: 100%;
  height: 100%;
  background: #1e293b;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  font-weight: 700;
  color: #fff;
  border: 2px solid rgba(255, 255, 255, 0.2);
}

.greeting {
  font-size: 24px;
  font-weight: 300;
  color: #fff;
  margin-bottom: 8px;
  letter-spacing: 1px;
}

.greeting span {
  font-weight: 700;
  background: linear-gradient(90deg, #e0c3fc 0%, #8ec5fc 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.user-id-tag {
  display: inline-block;
  padding: 4px 16px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 999px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.stats-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.1), transparent);
  margin: 0;
}

.stats-container {
  display: flex;
  justify-content: center;
}

.stat-box {
  padding: 16px;
  width: 100%;
}

.stat-icon {
  font-size: 24px;
  color: #8ec5fc;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
}

.action-btn-capsule {
  width: 100%;
  border-radius: 999px;
  height: 44px;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s;
}

.logout-btn {
  background: rgba(245, 108, 108, 0.1);
  border: 1px solid rgba(245, 108, 108, 0.2);
  color: #ff8a8a;
}

.logout-btn:hover {
  background: rgba(245, 108, 108, 0.9);
  color: #fff;
  box-shadow: 0 8px 20px rgba(245, 108, 108, 0.4);
  transform: translateY(-2px);
}

/* ================= 右侧：时光盲盒 ================= */
.blind-box-card {
  position: relative;
  padding: 0; /* 移除 padding，让图片铺满 */
}

.bb-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.8s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.blind-box-card:hover .bb-image {
  transform: scale(1.04);
}

.bb-header-tag {
  position: absolute;
  top: 24px;
  left: 24px;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  padding: 8px 16px;
  border-radius: 999px;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 1px;
  z-index: 10;
  border: 1px solid rgba(255, 255, 255, 0.15);
}

/* 交互遮罩与刷新按钮 */
.bb-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.4s ease;
  z-index: 2;
}

.blind-box-card:hover .bb-overlay {
  opacity: 1;
}

.roll-btn {
  transform: scale(0.5);
  transition: transform 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.6);
}

.blind-box-card:hover .roll-btn {
  transform: scale(1.2);
}

/* 底部沉浸式文字信息 */
.bb-info-layer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 100px 32px 32px;
  background: linear-gradient(to top, rgba(0,0,0,0.95) 0%, rgba(0,0,0,0.5) 50%, transparent 100%);
  z-index: 1;
  pointer-events: none;
}

.bb-text-content {
  transform: translateY(12px);
  transition: transform 0.4s ease;
}

.blind-box-card:hover .bb-text-content {
  transform: translateY(0);
}

.bb-quote {
  font-size: 20px;
  font-weight: 300;
  color: #fff;
  line-height: 1.6;
  margin-bottom: 16px;
  letter-spacing: 1px;
  text-shadow: 0 2px 4px rgba(0,0,0,0.8);
}

.bb-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bb-category {
  background: rgba(255, 255, 255, 0.2) !important;
  border: 1px solid rgba(255, 255, 255, 0.4) !important;
  backdrop-filter: blur(4px);
}

.bb-date {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  letter-spacing: 0.5px;
}

/* 空状态 */
.empty-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  color: rgba(255, 255, 255, 0.5);
  background: rgba(0, 0, 0, 0.2);
}

.empty-box .el-icon {
  margin-bottom: 24px;
  opacity: 0.5;
}

.empty-box p {
  font-size: 15px;
  line-height: 1.8;
  letter-spacing: 1px;
}

/* 响应式适配移动端 */
@media (max-width: 850px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
  .glass-card {
    height: auto;
    min-height: 400px;
  }
  .blind-box-card {
    height: 460px;
  }
}
</style>