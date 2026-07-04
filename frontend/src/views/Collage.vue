<template>
  <div class="collage-page">
    <div class="page-header-capsule">
      <h2>照片拼图</h2>
      <p class="hint">选择多张图片，套用模板生成拼图，可下载或上传到相册</p>
    </div>

    <div class="collage-layout">
      <div class="side-panel panel-capsule">
        <h3>图片列表</h3>
        <input ref="fileInput" type="file" accept="image/*" multiple hidden @change="onPickLocal" />
        <el-button type="primary" class="capsule-btn" @click="fileInput?.click()">从本地上传</el-button>
        <el-button class="capsule-btn" @click="openGalleryPicker">从相册选择</el-button>
        <el-button v-if="items.length" class="capsule-btn" @click="clearAll">清空全部</el-button>

        <div v-if="items.length" class="thumb-list">
          <div v-for="(item, index) in items" :key="item.id" class="thumb-item">
            <img :src="item.preview" :alt="item.name" />
            <div class="thumb-actions">
              <button class="mini-capsule" :disabled="index === 0" @click="moveItem(index, -1)">↑</button>
              <button class="mini-capsule" :disabled="index === items.length - 1" @click="moveItem(index, 1)">↓</button>
              <button class="mini-capsule danger" @click="removeItem(index)">删</button>
            </div>
            <span class="thumb-index">{{ index + 1 }}</span>
          </div>
        </div>
        <p v-else class="empty-tip">至少添加 2 张图片</p>
      </div>

      <div class="preview-area">
        <canvas ref="canvasRef"></canvas>
        <p v-if="!items.length" class="preview-placeholder">预览区</p>
      </div>

      <div class="side-panel options-panel panel-capsule">
        <h3>拼图设置</h3>
        <div class="option-item">
          <span>布局模板</span>
          <el-select v-model="layoutId" style="width: 100%" @change="render">
            <el-option
              v-for="layout in COLLAGE_LAYOUTS"
              :key="layout.id"
              :label="`${layout.name}（${layout.slots}格）`"
              :value="layout.id"
            />
          </el-select>
        </div>
        <div class="option-item">
          <span>输出尺寸</span>
          <el-select v-model="sizeId" style="width: 100%" @change="render">
            <el-option v-for="s in OUTPUT_SIZES" :key="s.id" :label="s.label" :value="s.id" />
          </el-select>
        </div>
        <div class="option-item">
          <span>间距 {{ gap }}px</span>
          <el-slider v-model="gap" :min="0" :max="24" @change="render" />
        </div>
        <div class="option-item">
          <span>圆角 {{ borderRadius }}px</span>
          <el-slider v-model="borderRadius" :min="0" :max="32" @change="render" />
        </div>
        <div class="option-item color-row">
          <span>背景色</span>
          <el-color-picker v-model="background" @change="render" />
        </div>

        <el-button type="success" class="capsule-btn" :disabled="items.length < 2" @click="downloadCollage">
          下载拼图
        </el-button>
        <el-button
          type="warning"
          class="capsule-btn"
          :disabled="items.length < 2"
          :loading="uploading"
          @click="uploadCollage"
        >
          上传到相册
        </el-button>
      </div>
    </div>

    <el-dialog v-model="galleryVisible" title="从相册选择图片" width="720px" class="gallery-dialog">
      <div v-loading="galleryLoading">
        <div v-if="galleryPhotos.length" class="gallery-pick-grid">
          <div
            v-for="photo in galleryPhotos"
            :key="photo.id"
            :class="['pick-card', { selected: isPicked(photo.id) }]"
            @click="togglePick(photo.id)"
          >
            <img :src="photo.thumbUrl || photo.url" :alt="photo.originalName" />
            <span v-if="isPicked(photo.id)" class="pick-badge">✓</span>
            <span class="pick-name">{{ photo.originalName }}</span>
          </div>
        </div>
        <el-empty v-else-if="!galleryLoading" description="相册暂无照片，请先上传" />
      </div>
      <template #footer>
        <div class="dialog-footer-capsule">
          <button class="capsule-action secondary" @click="galleryVisible = false">取消</button>
          <button
            class="capsule-action primary"
            :disabled="!pickedIds.length"
            @click="confirmGalleryPick"
          >
            添加已选（{{ pickedIds.length }}）
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPhotos, uploadPhotos } from '../api/photo'
import {
  COLLAGE_LAYOUTS,
  OUTPUT_SIZES,
  pickDefaultLayout,
  renderCollage,
  loadImageFromFile,
  loadImageFromUrl
} from '../utils/collageLayout'

const fileInput = ref(null)
const canvasRef = ref(null)
const items = ref([])
const layoutId = ref('2-h')
const sizeId = ref('1200x800')
const gap = ref(8)
const borderRadius = ref(0)
const background = ref('#ffffff')
const uploading = ref(false)

const galleryVisible = ref(false)
const galleryLoading = ref(false)
const galleryPhotos = ref([])
const pickedIds = ref([])

let uid = 0

const currentLayout = computed(() =>
  COLLAGE_LAYOUTS.find(l => l.id === layoutId.value) || COLLAGE_LAYOUTS[0]
)

const currentSize = computed(() =>
  OUTPUT_SIZES.find(s => s.id === sizeId.value) || OUTPUT_SIZES[0]
)

function isPicked(id) {
  return pickedIds.value.includes(id)
}

function render() {
  nextTick(() => {
    if (!canvasRef.value || items.value.length < 1) return
    const images = items.value.map(i => i.img)
    renderCollage(canvasRef.value, images, currentLayout.value, {
      outputWidth: currentSize.value.width,
      outputHeight: currentSize.value.height,
      gap: gap.value,
      background: background.value,
      borderRadius: borderRadius.value
    })
  })
}

async function addItems(newEntries) {
  items.value.push(...newEntries)
  const layout = pickDefaultLayout(items.value.length)
  if (layout) layoutId.value = layout.id
  render()
}

async function onPickLocal(e) {
  const files = Array.from(e.target.files || [])
  if (!files.length) return
  try {
    const loaded = await Promise.all(files.map(loadImageFromFile))
    const entries = loaded.map(({ img, name, file }) => ({
      id: ++uid,
      img,
      name,
      file,
      preview: img.src
    }))
    await addItems(entries)
    ElMessage.success(`已添加 ${entries.length} 张图片`)
  } catch {
    ElMessage.error('图片加载失败')
  }
  e.target.value = ''
}

function removeItem(index) {
  items.value.splice(index, 1)
  render()
}

function moveItem(index, dir) {
  const target = index + dir
  if (target < 0 || target >= items.value.length) return
  const list = [...items.value]
  ;[list[index], list[target]] = [list[target], list[index]]
  items.value = list
  render()
}

function clearAll() {
  items.value = []
}

function openGalleryPicker() {
  pickedIds.value = []
  galleryVisible.value = true
  loadGalleryPhotos()
}

async function loadGalleryPhotos() {
  galleryLoading.value = true
  try {
    const res = await getPhotos({ page: 1, pageSize: 200 })
    galleryPhotos.value = res.data?.records ?? []
  } catch {
    galleryPhotos.value = []
    ElMessage.error('加载相册失败')
  } finally {
    galleryLoading.value = false
  }
}

function togglePick(id) {
  const idx = pickedIds.value.indexOf(id)
  if (idx >= 0) {
    pickedIds.value.splice(idx, 1)
  } else {
    pickedIds.value.push(id)
  }
}

async function confirmGalleryPick() {
  const selected = galleryPhotos.value.filter(p => pickedIds.value.includes(p.id))
  if (!selected.length) return
  try {
    const loaded = await Promise.all(
      selected.map(p => loadImageFromUrl(`${p.url}?t=${Date.now()}`, p.originalName))
    )
    const entries = loaded.map(({ img, name }) => ({
      id: ++uid,
      img,
      name,
      file: null,
      preview: img.src
    }))
    await addItems(entries)
    galleryVisible.value = false
    pickedIds.value = []
    ElMessage.success(`已从相册添加 ${entries.length} 张`)
  } catch {
    ElMessage.error('相册图片加载失败')
  }
}

function downloadCollage() {
  if (!canvasRef.value) return
  const link = document.createElement('a')
  link.download = `collage-${Date.now()}.png`
  link.href = canvasRef.value.toDataURL('image/png')
  link.click()
  ElMessage.success('拼图已下载')
}

async function uploadCollage() {
  if (!canvasRef.value) return
  uploading.value = true
  try {
    const blob = await new Promise(resolve => canvasRef.value.toBlob(resolve, 'image/png'))
    const file = new File([blob], `collage-${Date.now()}.png`, { type: 'image/png' })
    await uploadPhotos([file])
    ElMessage.success('拼图已上传到相册')
  } catch {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

watch(items, () => render(), { deep: true })

onMounted(render)
</script>

<style scoped>
.collage-page {
  max-width: 1240px;
  margin: 0 auto;
  padding-bottom: 32px;
}

.page-header-capsule {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  width: fit-content;
  margin: 0 auto 24px;
  padding: 14px 28px;
  border-radius: 999px;
  background: rgba(18, 18, 28, 0.78);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.3);
}

.page-header-capsule h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 1px;
}

.hint {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: rgba(168, 216, 234, 0.95);
  letter-spacing: 0.5px;
}

.panel-capsule {
  padding: 16px 14px;
  border-radius: 16px;
  background: rgba(18, 18, 28, 0.78);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.28);
}

.collage-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  justify-content: center;
}

.side-panel {
  width: 200px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.side-panel h3 {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.92);
  margin-bottom: 4px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.capsule-btn {
  width: 100%;
  margin: 0;
  border-radius: 999px !important;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.panel-capsule :deep(.el-button) {
  --el-button-bg-color: rgba(255, 255, 255, 0.1);
  --el-button-border-color: rgba(255, 255, 255, 0.2);
  --el-button-text-color: rgba(255, 255, 255, 0.9);
  --el-button-hover-bg-color: rgba(255, 255, 255, 0.18);
  --el-button-hover-border-color: rgba(255, 255, 255, 0.3);
  --el-button-hover-text-color: #fff;
}

.panel-capsule :deep(.el-button--primary) {
  --el-button-bg-color: rgba(74, 144, 226, 0.85);
  --el-button-border-color: #4a90e2;
}

.panel-capsule :deep(.el-button--success) {
  --el-button-bg-color: rgba(103, 194, 58, 0.85);
  --el-button-border-color: #67c23a;
}

.panel-capsule :deep(.el-button--warning) {
  --el-button-bg-color: rgba(230, 162, 60, 0.85);
  --el-button-border-color: #e6a23c;
}

.panel-capsule :deep(.el-slider__runway) {
  background: rgba(255, 255, 255, 0.15);
}

.panel-capsule :deep(.el-slider__bar) {
  background: #4a90e2;
}

.panel-capsule :deep(.el-select .el-select__wrapper) {
  background: rgba(255, 255, 255, 0.08);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.15) inset;
  border-radius: 999px;
}

.panel-capsule :deep(.el-select .el-select__placeholder),
.panel-capsule :deep(.el-select .el-select__selected-item) {
  color: rgba(255, 255, 255, 0.9);
}

.option-item span {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 6px;
}

.option-item {
  margin-bottom: 14px;
}

.color-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.empty-tip {
  margin-top: 8px;
  font-size: 12px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.75);
  text-align: center;
  padding: 10px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.thumb-list {
  margin-top: 4px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 380px;
  overflow-y: auto;
}

.thumb-item {
  position: relative;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  padding: 6px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.thumb-item img {
  width: 100%;
  height: 72px;
  object-fit: cover;
  border-radius: 8px;
}

.thumb-actions {
  display: flex;
  gap: 6px;
  margin-top: 6px;
}

.mini-capsule {
  flex: 1;
  padding: 4px 0;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.9);
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.mini-capsule:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.2);
}

.mini-capsule:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.mini-capsule.danger {
  border-color: rgba(245, 108, 108, 0.5);
  color: #f89898;
}

.thumb-index {
  position: absolute;
  top: 10px;
  left: 10px;
  background: rgba(74, 144, 226, 0.95);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-area {
  flex: 1;
  min-height: 400px;
  max-width: 720px;
  background: repeating-conic-gradient(#eee 0% 25%, #fff 0% 50%) 50% / 20px 20px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.2), 0 4px 20px rgba(0, 0, 0, 0.2);
  overflow: auto;
}

.preview-area canvas {
  max-width: 100%;
  display: block;
}

.preview-placeholder {
  position: absolute;
  color: #999;
  font-size: 14px;
  font-weight: 500;
  padding: 8px 20px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.85);
}

.options-panel {
  width: 220px;
}

.gallery-pick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  min-height: 80px;
}

.pick-card {
  position: relative;
  cursor: pointer;
  border-radius: 12px;
  overflow: hidden;
  border: 2px solid transparent;
  transition: border-color 0.2s, transform 0.2s;
  background: #f5f5f5;
}

.pick-card:hover {
  transform: translateY(-2px);
}

.pick-card.selected {
  border-color: #4a90e2;
  box-shadow: 0 0 0 2px rgba(74, 144, 226, 0.25);
}

.pick-card img {
  width: 100%;
  height: 100px;
  object-fit: cover;
  display: block;
}

.pick-name {
  display: block;
  font-size: 11px;
  color: #666;
  padding: 4px 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pick-badge {
  position: absolute;
  top: 6px;
  right: 6px;
  background: #4a90e2;
  color: #fff;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}

.dialog-footer-capsule {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.capsule-action {
  padding: 10px 24px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.5px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.capsule-action.secondary {
  background: rgba(255, 255, 255, 0.9);
  border-color: #dcdfe6;
  color: #606266;
}

.capsule-action.secondary:hover {
  border-color: #4a90e2;
  color: #4a90e2;
}

.capsule-action.primary {
  background: #4a90e2;
  border-color: #4a90e2;
  color: #fff;
}

.capsule-action.primary:hover:not(:disabled) {
  background: #3a7bc8;
}

.capsule-action:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
</style>
