<template>
  <div class="gallery-page">
    <div class="gallery-top">
      <div class="gallery-header">
        <h1>Image Gallery</h1>
        <el-button type="primary" plain class="upload-btn-capsule" @click="$router.push('/home')">UPLOAD</el-button>
      </div>

      <div class="toolbar">
      <div class="tabs">
        <span
          class="tab"
          :class="{ active: activeCategory === 'ALL' }"
          @click="switchCategory('ALL')"
        >
          全部
        </span>
        <span
          v-for="cat in userCategories"
          :key="cat.id"
          class="tab tab-category"
          :class="{ active: activeCategory === cat.name }"
          @click="switchCategory(cat.name)"
        >
          {{ cat.name }}
          <span class="tab-edit" @click.stop="openEditKeywords(cat)" title="编辑关键词">⚙</span>
          <span
            v-if="!cat.builtIn"
            class="tab-remove"
            @click.stop="confirmDeleteCategory(cat)"
          >×</span>
        </span>
        <span class="tab tab-add" @click="showAddCategory = true">+ 新建</span>
      </div>
      <div class="search-capsule-shell">
        <el-input
          v-model="keyword"
          class="search-capsule"
          placeholder="搜索文件名或分类..."
          clearable
          @keyup.enter="onSearch"
          @clear="onSearch"
        >
          <template #append>
            <el-button @click="onSearch">搜索</el-button>
          </template>
        </el-input>
      </div>
    </div>
    </div>

    <div v-loading="loading" class="photo-grid">
      <div v-for="photo in photos" :key="photo.id" class="photo-card" @click="openPreview(photo)">
        <img :src="photo.thumbUrl || photo.url" :alt="photo.originalName" />
        <div class="photo-info">
          <el-tag size="small">{{ photo.aiCategory || '其他' }}</el-tag>
          <span class="name">{{ photo.originalName }}</span>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && !photos.length" description="暂无照片，去上传吧" />

    <div v-if="totalPages > 1" class="pagination-wrap">
      <div class="gallery-pagination">
        <button class="page-btn nav-btn" :disabled="page <= 1" @click="goPage(page - 1)">←</button>

        <template v-for="item in pageItems" :key="item.key">
          <span v-if="item.ellipsis" class="page-ellipsis">...</span>
          <button
            v-else
            class="page-btn"
            :class="{ active: item.page === page }"
            @click="goPage(item.page)"
          >
            {{ item.label }}
          </button>
        </template>

        <input
          v-model="jumpPage"
          class="page-jump"
          type="number"
          min="1"
          :max="totalPages"
          placeholder="#"
          @keyup.enter="onJumpGo"
        />
        <button class="page-btn go-btn" @click="onJumpGo">Go</button>

        <button class="page-btn nav-btn" :disabled="page >= totalPages" @click="goPage(page + 1)">→</button>
      </div>
    </div>

    <el-dialog v-model="previewVisible" title="照片详情" width="640px" @closed="resetPreviewView">
      <div v-if="currentPhoto" class="preview-content">
        <div
          class="preview-viewport"
          @wheel.prevent="onPreviewWheel"
          @mousedown="onPreviewPanStart"
          @mousemove="onPreviewPanMove"
          @mouseup="onPreviewPanEnd"
          @mouseleave="onPreviewPanEnd"
        >
          <img
            :src="currentPhoto.url"
            class="preview-img"
            :style="{ transform: `translate(${previewPanX}px, ${previewPanY}px) scale(${previewScale})` }"
            draggable="false"
          />
        </div>
        <p class="preview-zoom-tip">滚轮缩放 · 放大后可拖拽查看</p>
        <p><strong>文件名：</strong>{{ currentPhoto.originalName }}</p>
        <p><strong>AI 分类：</strong>{{ currentPhoto.aiCategory }}</p>
        <p><strong>置信度：</strong>{{ formatConfidence(currentPhoto.aiConfidence) }}</p>
        <p><strong>上传时间：</strong>{{ formatDateTime(currentPhoto.createdAt) }}</p>
        <div class="category-edit">
          <strong>修改分类：</strong>
          <el-select v-model="editCategory" style="width: 160px; margin: 0 8px">
            <el-option v-for="cat in categoryOptions" :key="cat" :label="cat" :value="cat" />
          </el-select>
          <el-button type="primary" size="small" @click="saveCategory">保存</el-button>
        </div>
        <div class="preview-actions">
          <el-button type="primary" @click="goEdit">编辑图片</el-button>
          <el-button type="danger" @click="handleDelete">删除照片</el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="showAddCategory" title="新建相册分类" width="440px" @closed="resetAddCategoryForm">
      <el-input v-model="newCategoryName" placeholder="分类名称（最多20字）" maxlength="20" />
      <el-input
        v-model="newCategoryKeywords"
        type="textarea"
        :rows="3"
        class="keyword-input"
        placeholder="AI 识别关键词（可选，逗号分隔）&#10;例：海滩,沙滩,旅行,度假"
      />
      <p class="keyword-hint">上传照片时，若 AI 识别结果或文件名包含这些词，将自动归入该分类</p>
      <template #footer>
        <el-button @click="showAddCategory = false">取消</el-button>
        <el-button type="primary" :loading="categorySaving" @click="handleAddCategory">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showEditKeywords" :title="`编辑关键词 - ${editingCategory?.name || ''}`" width="440px">
      <el-input
        v-model="editKeywordsText"
        type="textarea"
        :rows="4"
        placeholder="逗号分隔，如：海滩,沙滩,旅行"
      />
      <p class="keyword-hint">分类名称本身也会参与匹配，无需重复填写</p>
      <template #footer>
        <el-button @click="showEditKeywords = false">取消</el-button>
        <el-button type="primary" :loading="categorySaving" @click="handleSaveKeywords">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPhotos, updateCategory, deletePhoto } from '../api/photo'
import { getCategories, createCategory, deleteCategory, updateCategoryKeywords } from '../api/category'

const router = useRouter()
const route = useRoute()

const userCategories = ref([])
const categoryOptions = computed(() => userCategories.value.map(c => c.name))

const photos = ref([])
const loading = ref(false)
const activeCategory = ref('ALL')
const keyword = ref('')
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)
const jumpPage = ref('')
const showAddCategory = ref(false)
const newCategoryName = ref('')
const newCategoryKeywords = ref('')
const showEditKeywords = ref(false)
const editingCategory = ref(null)
const editKeywordsText = ref('')
const categorySaving = ref(false)
const previewVisible = ref(false)
const currentPhoto = ref(null)
const editCategory = ref('')
const previewScale = ref(1)
const previewPanX = ref(0)
const previewPanY = ref(0)
const previewPanning = ref(false)
let previewPanStart = { x: 0, y: 0 }
let previewPanOrigin = { x: 0, y: 0 }

onMounted(async () => {
  await loadCategories()
  if (route.query.category) {
    activeCategory.value = String(route.query.category)
  }
  await loadPhotos()
})

async function loadCategories() {
  const res = await getCategories()
  userCategories.value = res.data
}

function parseKeywordsInput(text) {
  return String(text || '')
    .split(/[,，、\s]+/)
    .map(item => item.trim())
    .filter(Boolean)
}

function resetAddCategoryForm() {
  newCategoryName.value = ''
  newCategoryKeywords.value = ''
}

function openEditKeywords(cat) {
  editingCategory.value = cat
  editKeywordsText.value = (cat.keywords || []).join('，')
  showEditKeywords.value = true
}

async function handleAddCategory() {
  const name = newCategoryName.value.trim()
  if (!name) {
    ElMessage.warning('请输入分类名称')
    return
  }
  categorySaving.value = true
  try {
    await createCategory(name, parseKeywordsInput(newCategoryKeywords.value))
    ElMessage.success('分类创建成功')
    showAddCategory.value = false
    resetAddCategoryForm()
    await loadCategories()
  } finally {
    categorySaving.value = false
  }
}

async function handleSaveKeywords() {
  if (!editingCategory.value) return
  categorySaving.value = true
  try {
    await updateCategoryKeywords(editingCategory.value.id, parseKeywordsInput(editKeywordsText.value))
    ElMessage.success('关键词已更新')
    showEditKeywords.value = false
    await loadCategories()
  } finally {
    categorySaving.value = false
  }
}

async function confirmDeleteCategory(cat) {
  const count = cat.photoCount || 0
  const msg = count > 0
    ? `分类「${cat.name}」下有 ${count} 张照片，如何处理？`
    : `确定删除分类「${cat.name}」吗？`

  try {
    if (count > 0) {
      await ElMessageBox.confirm(msg, '删除分类', {
        distinguishCancelAndClose: true,
        confirmButtonText: '删除全部照片',
        cancelButtonText: '保留照片（移至其他）',
        type: 'warning'
      })
      await deleteCategory(cat.id, true)
    } else {
      await ElMessageBox.confirm(msg, '删除分类', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      })
      await deleteCategory(cat.id, false)
    }
    ElMessage.success('分类已删除')
    if (activeCategory.value === cat.name) {
      activeCategory.value = 'ALL'
      page.value = 1
    }
    await loadCategories()
    await loadPhotos()
  } catch (e) {
    if (e === 'cancel' && count > 0) {
      await deleteCategory(cat.id, false)
      ElMessage.success('分类已删除，照片已移至「其他」')
      if (activeCategory.value === cat.name) {
        activeCategory.value = 'ALL'
        page.value = 1
      }
      await loadCategories()
      await loadPhotos()
    }
  }
}

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const pageItems = computed(() => buildPageItems(page.value, totalPages.value))

function buildPageItems(current, last) {
  if (last <= 9) {
    return Array.from({ length: last }, (_, i) => ({
      key: `p-${i + 1}`,
      page: i + 1,
      label: String(i + 1)
    }))
  }

  const items = []

  if (current <= 4) {
    for (let i = 1; i <= 7; i++) {
      items.push({ key: `p-${i}`, page: i, label: String(i) })
    }
    items.push({ key: 'ellipsis', ellipsis: true })
    items.push({ key: `p-${last}`, page: last, label: String(last) })
    return items
  }

  if (current >= last - 3) {
    items.push({ key: 'p-1', page: 1, label: '1' })
    items.push({ key: 'ellipsis', ellipsis: true })
    for (let i = last - 6; i <= last; i++) {
      items.push({ key: `p-${i}`, page: i, label: String(i) })
    }
    return items
  }

  items.push({ key: 'p-1', page: 1, label: '1' })
  items.push({ key: 'ellipsis-left', ellipsis: true })
  for (let i = current - 2; i <= current + 2; i++) {
    items.push({ key: `p-${i}`, page: i, label: String(i) })
  }
  items.push({ key: 'ellipsis-right', ellipsis: true })
  items.push({ key: `p-${last}`, page: last, label: String(last) })
  return items
}

async function loadPhotos() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (activeCategory.value !== 'ALL') params.category = activeCategory.value
    if (keyword.value) params.keyword = keyword.value
    const res = await getPhotos(params)
    photos.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 1
  loadPhotos()
}

function goPage(nextPage) {
  const target = Math.min(Math.max(nextPage, 1), totalPages.value)
  if (target === page.value) return
  page.value = target
  loadPhotos()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function onJumpGo() {
  const target = Number(jumpPage.value)
  if (!Number.isInteger(target) || target < 1 || target > totalPages.value) {
    ElMessage.warning(`请输入 1 到 ${totalPages.value} 之间的页码`)
    return
  }
  jumpPage.value = ''
  goPage(target)
}

function switchCategory(cat) {
  activeCategory.value = cat
  page.value = 1
  loadPhotos()
}

function openPreview(photo) {
  currentPhoto.value = photo
  editCategory.value = photo.aiCategory || '其他'
  resetPreviewView()
  previewVisible.value = true
}

function resetPreviewView() {
  previewScale.value = 1
  previewPanX.value = 0
  previewPanY.value = 0
}

function onPreviewWheel(event) {
  const delta = event.deltaY > 0 ? -0.12 : 0.12
  previewScale.value = Math.min(4, Math.max(0.5, previewScale.value + delta))
}

function onPreviewPanStart(event) {
  if (previewScale.value <= 1) return
  previewPanning.value = true
  previewPanStart = { x: event.clientX, y: event.clientY }
  previewPanOrigin = { x: previewPanX.value, y: previewPanY.value }
}

function onPreviewPanMove(event) {
  if (!previewPanning.value) return
  previewPanX.value = previewPanOrigin.x + (event.clientX - previewPanStart.x)
  previewPanY.value = previewPanOrigin.y + (event.clientY - previewPanStart.y)
}

function onPreviewPanEnd() {
  previewPanning.value = false
}

function goEdit() {
  previewVisible.value = false
  router.push({ path: '/edit', query: { photoId: currentPhoto.value.id } })
}

async function saveCategory() {
  await updateCategory(currentPhoto.value.id, editCategory.value)
  currentPhoto.value.aiCategory = editCategory.value
  ElMessage.success('分类已更新')
  loadPhotos()
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定要删除这张照片吗？删除后无法恢复。', '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deletePhoto(currentPhoto.value.id)
    ElMessage.success('照片已删除')
    previewVisible.value = false
    currentPhoto.value = null
    if (photos.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    loadPhotos()
  } catch (e) {
    if (e !== 'cancel') {
      throw e
    }
  }
}

function formatConfidence(val) {
  return val ? (val * 100).toFixed(1) + '%' : '-'
}

function formatDateTime(val) {
  if (!val) return '-'
  return String(val).replace('T', ' ')
}
</script>

<style scoped>
.gallery-page {
  max-width: 1200px;
  margin: 0 auto;
}

.gallery-top {
  padding: 0;
  margin-bottom: 24px;
  background: transparent;
  border: none;
  box-shadow: none;
}

.gallery-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.gallery-header h1 {
  font-size: 32px;
  font-weight: 600;
  letter-spacing: 2px;
  color: #1e293b;
  text-shadow: 0 1px 3px rgba(255, 255, 255, 0.95), 0 0 12px rgba(255, 255, 255, 0.8);
}

.upload-btn-capsule {
  --el-button-bg-color: rgba(18, 18, 28, 0.82);
  --el-button-border-color: rgba(255, 255, 255, 0.1);
  --el-button-text-color: #6eb5ff;
  --el-button-hover-bg-color: rgba(28, 28, 42, 0.92);
  --el-button-hover-border-color: rgba(110, 181, 255, 0.35);
  --el-button-hover-text-color: #fff;
  border-radius: 999px;
  padding: 10px 28px;
  font-weight: 600;
  letter-spacing: 1.5px;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow:
    0 4px 20px rgba(0, 0, 0, 0.35),
    0 0 0 1px rgba(255, 255, 255, 0.04) inset;
  transition: all 0.2s;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tab {
  padding: 8px 18px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(18, 18, 28, 0.78);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  color: rgba(255, 255, 255, 0.78);
  border-radius: 999px;
  cursor: pointer;
  font-size: 13px;
  letter-spacing: 1px;
  transition: all 0.2s;
  box-shadow:
    0 4px 16px rgba(0, 0, 0, 0.28),
    0 0 0 1px rgba(255, 255, 255, 0.04) inset;
}

.tab:hover {
  border-color: rgba(110, 181, 255, 0.35);
  color: rgba(255, 255, 255, 0.95);
  background: rgba(28, 28, 42, 0.85);
}

.tab.active {
  border-color: #4a90e2;
  color: #fff;
  background: #4a90e2;
  font-weight: 600;
  box-shadow:
    0 4px 16px rgba(74, 144, 226, 0.35),
    0 0 0 1px rgba(255, 255, 255, 0.1) inset;
}

.tab-category {
  position: relative;
  padding-right: 48px;
}

.tab-edit {
  position: absolute;
  right: 24px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 12px;
  opacity: 0.55;
  cursor: pointer;
  transition: opacity 0.15s;
}

.tab-edit:hover {
  opacity: 1;
}

.tab.active .tab-edit {
  opacity: 0.85;
}

.keyword-input {
  margin-top: 12px;
}

.keyword-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.5;
}

.tab-remove {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  width: 16px;
  height: 16px;
  line-height: 14px;
  text-align: center;
  border-radius: 50%;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.45);
  transition: all 0.15s;
}

.tab.active .tab-remove {
  color: rgba(255, 255, 255, 0.85);
}

.tab-remove:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #ff8a8a;
}

.tab.active .tab-remove:hover {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
}

.tab-add {
  border-style: dashed;
  border-color: rgba(110, 181, 255, 0.35);
  color: #6eb5ff;
  background: rgba(18, 18, 28, 0.65);
}

.tab-add:hover {
  border-color: rgba(110, 181, 255, 0.55);
  background: rgba(28, 28, 42, 0.85);
  color: #fff;
}

.search-capsule-shell {
  flex-shrink: 0;
  width: 300px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(42, 42, 58, 0.98) 0%, rgba(16, 16, 26, 0.98) 100%);
  border: 1px solid rgba(255, 255, 255, 0.16);
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.45),
    0 0 0 1px rgba(255, 255, 255, 0.06) inset;
  overflow: hidden;
}

.search-capsule {
  width: 100%;
  --el-input-bg-color: transparent;
  --el-input-border-color: transparent;
  --el-input-hover-border-color: transparent;
  --el-input-focus-border-color: transparent;
  --el-fill-color-blank: transparent;
}

.search-capsule :deep(.el-input-group) {
  border-radius: 999px;
  overflow: hidden;
  background: transparent;
  box-shadow: none;
}

.search-capsule :deep(.el-input__wrapper) {
  border-radius: 0;
  box-shadow: none !important;
  background: transparent !important;
  padding-left: 16px;
}

.search-capsule :deep(.el-input__wrapper.is-focus) {
  box-shadow: none !important;
}

.search-capsule :deep(.el-input__wrapper input),
.search-capsule :deep(.el-input__inner) {
  color: rgba(255, 255, 255, 0.95);
}

.search-capsule :deep(.el-input__wrapper input::placeholder),
.search-capsule :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.55);
}

.search-capsule :deep(.el-input__suffix .el-input__clear) {
  color: rgba(255, 255, 255, 0.65);
}

.search-capsule :deep(.el-input-group__append) {
  border-radius: 0;
  box-shadow: none;
  background: rgba(255, 255, 255, 0.1) !important;
  padding: 0;
  border-left: 1px solid rgba(255, 255, 255, 0.12) !important;
}

.search-capsule :deep(.el-input-group__append .el-button) {
  border: none;
  border-radius: 0;
  margin: 0;
  padding: 0 18px;
  height: 100%;
  color: #8ec5ff;
  font-weight: 600;
  background: transparent !important;
}

.search-capsule :deep(.el-input-group__append .el-button:hover) {
  color: #fff;
  background: rgba(110, 181, 255, 0.2) !important;
}

.photo-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  min-height: 200px;
}

.photo-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s;
}

.photo-card:hover {
  transform: translateY(-2px);
}

.photo-card img {
  width: 100%;
  height: 180px;
  object-fit: cover;
}

.photo-info {
  padding: 8px 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.name {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-viewport {
  height: 360px;
  overflow: hidden;
  background: #f5f5f5;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
  cursor: grab;
}

.preview-img {
  max-width: 100%;
  max-height: 340px;
  object-fit: contain;
  transform-origin: center center;
  user-select: none;
}

.preview-zoom-tip {
  text-align: center;
  font-size: 12px;
  color: #999;
  margin-bottom: 12px;
}

.category-edit {
  margin-top: 16px;
  display: flex;
  align-items: center;
}

.preview-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

.gallery-pagination {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: linear-gradient(180deg, rgba(70, 70, 70, 0.92) 0%, rgba(35, 35, 35, 0.95) 100%);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 999px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.35);
}

.page-btn {
  min-width: 36px;
  height: 36px;
  padding: 0 8px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(180deg, #5a5a5a 0%, #383838 100%);
  color: #fff;
  font-size: 13px;
  line-height: 1;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s, opacity 0.15s;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12), 0 2px 6px rgba(0, 0, 0, 0.25);
}

.page-btn:hover:not(:disabled):not(.active) {
  transform: translateY(-1px);
  background: linear-gradient(180deg, #666 0%, #444 100%);
}

.page-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.page-btn.active {
  box-shadow: 0 0 0 2px #fff, 0 0 14px rgba(255, 255, 255, 0.45);
  font-weight: 600;
}

.page-btn.nav-btn {
  font-size: 16px;
}

.page-btn.go-btn {
  font-size: 12px;
  letter-spacing: 0.5px;
}

.page-ellipsis {
  color: rgba(255, 255, 255, 0.75);
  min-width: 24px;
  text-align: center;
  font-size: 14px;
  user-select: none;
}

.page-jump {
  width: 44px;
  height: 36px;
  border: none;
  border-radius: 18px;
  background: linear-gradient(180deg, #5a5a5a 0%, #383838 100%);
  color: #fff;
  text-align: center;
  font-size: 13px;
  outline: none;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12), 0 2px 6px rgba(0, 0, 0, 0.25);
}

.page-jump::placeholder {
  color: rgba(255, 255, 255, 0.45);
}

.page-jump::-webkit-outer-spin-button,
.page-jump::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
</style>
