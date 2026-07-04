<template>
  <div class="home-landing">
    <section class="hero">
      <div class="hero-bg" />
      <div class="hero-content">
        <h1 class="hero-title">欢迎来到码影拾光</h1>
        <p class="hero-subtitle">拍下過 記住過 好過擁有</p>
        <p class="hero-tagline">有人性 鏡頭裡總有豐收</p>
      </div>
      <button class="scroll-down" aria-label="向下滚动" @click="scrollToFeatured">↓</button>
    </section>

    <section ref="featuredRef" class="featured">
      <div class="featured-bg" aria-hidden="true" />
      <div class="featured-overlay" aria-hidden="true" />
      <div class="featured-inner">
        <div
          v-for="cat in featuredCategories"
          :key="cat.id"
          class="album-card"
          @click="goToCategory(cat.name)"
        >
          <div class="card-cover">
            <img v-if="cat.coverUrl" :src="cat.coverUrl" :alt="cat.name" />
            <div v-else class="cover-placeholder" :style="{ background: cat.placeholderColor }">
              {{ cat.name.charAt(0) }}
            </div>
          </div>
          <h3 class="card-title">{{ cat.name }}</h3>
          <router-link
            class="card-link"
            :to="{ path: '/gallery', query: { category: cat.name } }"
            @click.stop
          >
            查看{{ cat.name }}
          </router-link>
        </div>
        <el-empty
          v-if="!loadingCategories && !featuredCategories.length"
          description="暂无分类，前往 Gallery 创建相册分类"
          class="featured-empty"
        />
      </div>
    </section>

    <section id="upload" class="upload-section">
      <div class="upload-inner">
        <h2>上传你的照片</h2>
        <p class="hint">支持批量上传，上传后 AI 将自动识别分类</p>

        <input ref="fileInput" type="file" accept="image/*" multiple hidden @change="onFileChange" />

        <el-button type="primary" size="large" class="action-btn action-btn-capsule" @click="fileInput.click()">
          选择你的图片（可批量）
        </el-button>

        <div v-if="selectedFiles.length" class="file-list">
          已选 {{ selectedFiles.length }} 张：
          <span v-for="(f, i) in selectedFiles" :key="i">{{ f.name }} </span>
        </div>

        <el-button
          type="success"
          size="large"
          class="action-btn action-btn-capsule action-btn-upload"
          :loading="uploading"
          :disabled="!selectedFiles.length"
          @click="handleUpload"
        >
          上传
        </el-button>

        <div v-if="uploadResults.length" class="results">
          <h3>AI 分类结果</h3>
          <el-table :data="uploadResults" stripe>
            <el-table-column prop="originalName" label="文件名" />
            <el-table-column prop="aiCategory" label="AI 分类" width="120">
              <template #default="{ row }">
                <el-tag :type="categoryTagType(row.aiCategory)">{{ row.aiCategory }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="置信度" width="100">
              <template #default="{ row }">
                {{ row.aiConfidence ? (row.aiConfidence * 100).toFixed(1) + '%' : '-' }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
// 【修改点 2】：引入 useRouter
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { uploadPhotos, getPhotos } from '../api/photo'
import { getCategories } from '../api/category'

// 【修改点 3】：初始化 router
const router = useRouter()

const featuredRef = ref(null)
const fileInput = ref(null)
const selectedFiles = ref([])
const uploading = ref(false)
const uploadResults = ref([])
const featuredCategories = ref([])
const loadingCategories = ref(true)

const PLACEHOLDER_COLORS = [
  'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)'
]

onMounted(loadFeaturedCategories)

async function loadFeaturedCategories() {
  loadingCategories.value = true
  try {
    const res = await getCategories()
    const list = res.data.filter(c => c.name !== '其他').slice(0, 4)
    const withCovers = await Promise.all(
      list.map(async (cat, index) => {
        let coverUrl = ''
        try {
          const photoRes = await getPhotos({ category: cat.name, page: 1, pageSize: 1 })
          const photo = photoRes.data.records?.[0]
          coverUrl = photo?.thumbUrl || photo?.url || ''
        } catch {
          coverUrl = ''
        }
        return {
          ...cat,
          coverUrl,
          placeholderColor: PLACEHOLDER_COLORS[index % PLACEHOLDER_COLORS.length]
        }
      })
    )
    featuredCategories.value = withCovers
  } finally {
    loadingCategories.value = false
  }
}

function scrollToFeatured() {
  featuredRef.value?.scrollIntoView({ behavior: 'smooth' })
}

function onFileChange(e) {
  selectedFiles.value = Array.from(e.target.files)
}

async function handleUpload() {
  uploading.value = true
  try {
    const res = await uploadPhotos(selectedFiles.value)
    uploadResults.value = res.data.photos
    ElMessage.success(`成功上传 ${res.data.uploaded} 张照片`)
    selectedFiles.value = []
    fileInput.value.value = ''
    await loadFeaturedCategories()
  } finally {
    uploading.value = false
  }
}

function categoryTagType(cat) {
  const map = {
    食物: 'success', 风景: '', 节日: 'warning', 人像: 'danger',
    动漫: 'info', 设计: 'info', 动物: 'warning', 其他: 'info'
  }
  return map[cat] || 'info'
}

// 【修改点 4】：新增点击整张卡片跳转的函数
function goToCategory(categoryName) {
  router.push({
    path: '/gallery',
    query: { category: categoryName }
  })
}
</script>

<style scoped>
.home-landing {
  min-height: 100vh;
}

.hero {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding-top: 64px;
}

.hero-bg {
  position: absolute;
  inset: 0;
  background: url('/app-bg.png') center / cover no-repeat;
  filter: brightness(0.45);
}

.hero-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    180deg,
    rgba(15, 20, 25, 0.3) 0%,
    rgba(15, 20, 25, 0.55) 60%,
    rgba(15, 20, 25, 0.85) 100%
  );
}

.hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: 0 24px 80px;
  max-width: 900px;
}

.hero-title {
  font-size: clamp(36px, 6vw, 64px);
  font-weight: 700;
  color: #fff;
  letter-spacing: 4px;
  margin-bottom: 24px;
  text-shadow: 0 4px 24px rgba(0, 0, 0, 0.4);
}

.hero-subtitle {
  font-size: clamp(18px, 2.5vw, 26px);
  color: rgba(255, 255, 255, 0.92);
  letter-spacing: 6px;
  margin-bottom: 16px;
  font-weight: 300;
}

.hero-tagline {
  font-size: clamp(14px, 1.8vw, 18px);
  color: rgba(168, 216, 234, 0.88);
  letter-spacing: 3px;
}

.scroll-down {
  position: absolute;
  bottom: 32px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 2;
  background: none;
  border: none;
  color: rgba(168, 216, 234, 0.9);
  font-size: 28px;
  cursor: pointer;
  animation: bounce 2s infinite;
  padding: 8px 16px;
  transition: color 0.2s;
}

.scroll-down:hover {
  color: #fff;
}

@keyframes bounce {
  0%, 100% { transform: translateX(-50%) translateY(0); }
  50% { transform: translateX(-50%) translateY(10px); }
}

.featured {
  position: relative;
  padding: 56px 48px 64px;
  overflow: hidden;
}

.featured-bg {
  position: absolute;
  inset: 0;
  background: url('/featured-bg.png') center / cover no-repeat;
  background-attachment: scroll;
}

.featured-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    180deg,
    rgba(12, 18, 32, 0.25) 0%,
    rgba(12, 18, 32, 0.4) 50%,
    rgba(12, 18, 32, 0.55) 100%
  );
}

.featured-inner {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 28px;
  justify-items: center;
}

.featured-empty {
  grid-column: 1 / -1;
  padding: 40px 0;
}

.featured-empty :deep(.el-empty__description) {
  color: rgba(255, 255, 255, 0.75);
}

.album-card {
  width: 100%;
  max-width: 260px;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 80, 120, 0.12);
  transition: transform 0.25s, box-shadow 0.25s;
  /* 【修改点 5】：增加 pointer 光标，让用户知道整张卡片都可以点 */
  cursor: pointer;
}

.album-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 40px rgba(0, 80, 120, 0.18);
}

.card-cover {
  aspect-ratio: 1;
  overflow: hidden;
  background: #eef5f8;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.9);
}

.card-title {
  padding: 16px 16px 8px;
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
}

.card-link {
  display: inline-block;
  padding: 0 16px 16px;
  font-size: 14px;
  color: #4a90e2;
  transition: color 0.2s;
}

.card-link:hover {
  color: #2563eb;
  text-decoration: underline;
}

.upload-section {
  padding: 64px 48px 80px;
  background: rgba(15, 20, 25, 0.95);
}

.upload-inner {
  max-width: 700px;
  margin: 0 auto;
  text-align: center;
}

.upload-inner h2 {
  font-size: 28px;
  color: #fff;
  margin-bottom: 8px;
}

.hint {
  color: rgba(168, 216, 234, 0.75);
  margin-bottom: 32px;
}

.action-btn {
  display: block;
  width: 320px;
  max-width: 100%;
  margin: 16px auto;
  height: 48px;
  font-size: 16px;
}

.action-btn-capsule {
  border-radius: 999px;
  font-weight: 600;
  letter-spacing: 0.5px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow:
    0 4px 20px rgba(0, 0, 0, 0.35),
    0 0 0 1px rgba(255, 255, 255, 0.04) inset;
  transition: background 0.2s, box-shadow 0.2s, transform 0.15s;
}

.action-btn-capsule:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow:
    0 6px 24px rgba(0, 0, 0, 0.4),
    0 0 20px rgba(100, 120, 255, 0.15);
}

.action-btn-capsule.el-button--primary {
  --el-button-bg-color: rgba(74, 144, 226, 0.88);
  --el-button-border-color: rgba(110, 181, 255, 0.35);
  --el-button-hover-bg-color: rgba(58, 123, 200, 0.95);
  --el-button-hover-border-color: rgba(110, 181, 255, 0.5);
  --el-button-text-color: #fff;
}

.action-btn-upload.el-button--success {
  --el-button-bg-color: rgba(34, 160, 90, 0.88);
  --el-button-border-color: rgba(74, 222, 128, 0.35);
  --el-button-hover-bg-color: rgba(22, 130, 72, 0.95);
  --el-button-hover-border-color: rgba(74, 222, 128, 0.5);
  --el-button-text-color: #fff;
}

.action-btn-capsule.is-disabled {
  opacity: 0.45;
}

.file-list {
  margin: 16px 0;
  color: rgba(255, 255, 255, 0.65);
  font-size: 14px;
}

.results {
  margin-top: 40px;
  text-align: left;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 8px;
  padding: 20px;
}

.results h3 {
  margin-bottom: 16px;
  color: #333;
}

@media (max-width: 768px) {
  .featured {
    padding: 40px 20px 48px;
  }

  .featured-inner {
    grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
    gap: 16px;
  }

  .upload-section {
    padding: 48px 20px 64px;
  }
}
</style>