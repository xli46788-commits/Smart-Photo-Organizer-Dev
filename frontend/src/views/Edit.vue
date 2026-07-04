<template>
  <div class="edit-page">
    <div class="page-header-capsule">
      <h2>照片编辑</h2>
      <p class="hint">
        {{ editingPhotoName ? `正在编辑：${editingPhotoName}` : '支持缩放查看、滤镜、马赛克画笔、裁剪' }}
      </p>
    </div>

    <div class="edit-layout">
      <div class="side-panel panel-capsule">
        <h3>基础调整</h3>
        <el-button class="edit-btn" @click="resetAll">恢复原图</el-button>
        <el-button class="edit-btn" @click="rotate(-90)">逆时针90°</el-button>
        <el-button class="edit-btn" @click="rotate(90)">顺时针90°</el-button>
        <el-button class="edit-btn" @click="toggleFlip('flipH')">左右翻转</el-button>
        <el-button class="edit-btn" @click="toggleFlip('flipV')">上下翻转</el-button>

        <h3 class="section-gap">工具模式</h3>
        <el-radio-group v-model="toolMode" size="small" class="tool-group">
          <el-radio-button value="view">查看</el-radio-button>
          <el-radio-button value="mosaic">马赛克</el-radio-button>
          <el-radio-button value="eraser">擦除</el-radio-button>
          <el-radio-button value="crop">裁剪</el-radio-button>
        </el-radio-group>
        <p class="tool-tip">{{ toolTip }}</p>
      </div>

      <div
        ref="viewportRef"
        class="canvas-viewport"
        :class="viewportClass"
        @wheel.prevent="onWheel"
        @mousedown="onPointerDown"
        @mousemove="onPointerMove"
        @mouseup="onPointerUp"
        @mouseleave="onPointerUp"
      >
        <input ref="fileInput" type="file" accept="image/*" hidden @change="loadImage" />
        <div v-show="!imageLoaded" class="placeholder" @click="fileInput.click()">
          <el-icon :size="48"><Upload /></el-icon>
          <p>点击上传你的图片</p>
        </div>
        <div
          v-show="imageLoaded"
          class="canvas-stage"
          :style="{ transform: `translate(${viewPanX}px, ${viewPanY}px) scale(${viewScale})` }"
        >
          <canvas ref="canvasRef"></canvas>
          <div
            v-if="toolMode === 'crop' && cropRect"
            class="crop-box"
            :style="cropBoxStyle"
          ></div>
        </div>
      </div>

      <div class="side-panel adjust-panel panel-capsule">
        <h3>精细调节</h3>
        <div class="slider-item">
          <span>亮度</span>
          <el-slider v-model="editState.brightness" :min="-50" :max="50" @change="onManualAdjust" />
        </div>
        <div class="slider-item">
          <span>对比度</span>
          <el-slider v-model="editState.contrast" :min="-50" :max="50" @change="onManualAdjust" />
        </div>
        <div class="slider-item">
          <span>饱和度</span>
          <el-slider v-model="editState.saturation" :min="0" :max="200" @change="onManualAdjust" />
        </div>
        <div class="slider-item">
          <span>模糊</span>
          <el-slider v-model="editState.blur" :min="0" :max="10" :step="0.5" @change="onManualAdjust" />
        </div>

        <template v-if="toolMode === 'mosaic' || toolMode === 'eraser'">
          <h3 class="section-gap">画笔设置</h3>
          <div class="slider-item">
            <span>画笔大小（{{ brushSize }}px）</span>
            <el-slider v-model="brushSize" :min="2" :max="80" :step="1" />
          </div>
          <div v-if="toolMode === 'mosaic'" class="slider-item">
            <span>马赛克强度</span>
            <el-slider v-model="mosaicBlockSize" :min="4" :max="24" :step="2" />
          </div>
        </template>

        <template v-if="toolMode === 'view'">
          <h3 class="section-gap">缩放</h3>
          <div class="slider-item">
            <span>当前 {{ Math.round(viewScale * 100) }}%</span>
            <el-slider v-model="viewScale" :min="0.5" :max="4" :step="0.1" />
          </div>
          <el-button class="edit-btn" @click="resetView">重置视图</el-button>
        </template>

        <template v-if="toolMode === 'crop'">
          <h3 class="section-gap">裁剪</h3>
          <el-button class="edit-btn" type="primary" :disabled="!cropRect" @click="applyCrop">
            确认裁剪
          </el-button>
          <el-button class="edit-btn" @click="clearCrop">取消选区</el-button>
        </template>
      </div>
    </div>

    <div v-if="imageLoaded" class="filter-section panel-capsule">
      <h3>滤镜选择</h3>
      <div class="filter-grid">
        <button
          v-for="preset in filterPresets"
          :key="preset.id"
          :class="['filter-chip', { active: editState.preset === preset.id }]"
          @click="selectPreset(preset.id)"
        >
          {{ preset.name }}
        </button>
      </div>
    </div>

    <div class="bottom-actions">
      <el-button type="primary" size="large" @click="fileInput.click()">上传你的图片</el-button>
      <el-button v-if="editingPhotoId" size="large" @click="backToGallery">返回相册</el-button>
      <el-button v-if="editingPhotoId" type="warning" size="large" :disabled="!imageLoaded" :loading="saving" @click="saveOverwrite">
        保存并覆盖原图
      </el-button>
      <el-button type="success" size="large" :disabled="!imageLoaded" @click="downloadImage">
        下载到本地
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPhoto, saveEditedPhoto } from '../api/photo'
import {
  FILTER_PRESETS,
  applyPreset,
  canvasToImage,
  compositeWithMosaic,
  createDefaultEditState,
  createMaskCanvas,
  paintMaskCircle,
  pixelateRegion,
  renderToOffscreenCanvas,
  resetEditState,
  restoreRegion
} from '../utils/imageFilters'

const route = useRoute()
const router = useRouter()

const fileInput = ref(null)
const canvasRef = ref(null)
const viewportRef = ref(null)
const imageLoaded = ref(false)
const editingPhotoId = ref(null)
const editingPhotoName = ref('')
const saving = ref(false)
const filterPresets = FILTER_PRESETS
const editState = reactive(createDefaultEditState())

const toolMode = ref('view')
const viewScale = ref(1)
const viewPanX = ref(0)
const viewPanY = ref(0)
const brushSize = ref(16)
const mosaicBlockSize = ref(12)

const cropRect = ref(null)
const cropStart = ref(null)

let originalImage = null
let sourceCanvas = null
let maskCanvas = null
let isDrawing = false
let isPanning = false
let panStart = { x: 0, y: 0 }
let panOrigin = { x: 0, y: 0 }

const toolTip = computed(() => {
  const tips = {
    view: '滚轮缩放，放大后可拖拽平移',
    mosaic: '在图片上按住鼠标涂抹添加马赛克',
    eraser: '在图片上按住鼠标擦除马赛克',
    crop: '拖拽框选裁剪区域，再点确认裁剪'
  }
  return tips[toolMode.value]
})

const viewportClass = computed(() => ({
  'is-crop': toolMode.value === 'crop',
  'is-pan': toolMode.value === 'view' && viewScale.value > 1,
  'is-brush': toolMode.value === 'mosaic' || toolMode.value === 'eraser'
}))

const cropBoxStyle = computed(() => {
  if (!cropRect.value || !canvasRef.value) return {}
  const canvas = canvasRef.value
  const displayW = canvas.clientWidth || canvas.width
  const displayH = canvas.clientHeight || canvas.height
  if (!displayW || !displayH) return {}
  const scaleX = displayW / canvas.width
  const scaleY = displayH / canvas.height
  const { x, y, w, h } = cropRect.value
  return {
    left: `${x * scaleX}px`,
    top: `${y * scaleY}px`,
    width: `${w * scaleX}px`,
    height: `${h * scaleY}px`
  }
})

function resetView() {
  viewScale.value = 1
  viewPanX.value = 0
  viewPanY.value = 0
}

function resetLayers() {
  sourceCanvas = null
  maskCanvas = null
  cropRect.value = null
  resetView()
}

function rebuildSource() {
  if (!originalImage) return
  sourceCanvas = renderToOffscreenCanvas(originalImage, editState)
  if (!maskCanvas || maskCanvas.width !== sourceCanvas.width || maskCanvas.height !== sourceCanvas.height) {
    maskCanvas = createMaskCanvas(sourceCanvas.width, sourceCanvas.height)
  }
}

async function drawCanvas() {
  if (!canvasRef.value || !sourceCanvas || !maskCanvas) return
  compositeWithMosaic(sourceCanvas, maskCanvas, canvasRef.value, mosaicBlockSize.value)
}

async function refreshCanvas() {
  rebuildSource()
  await nextTick()
  drawCanvas()
}

function resetAll() {
  Object.assign(editState, createDefaultEditState())
  if (sourceCanvas && maskCanvas) {
    maskCanvas.getContext('2d').clearRect(0, 0, maskCanvas.width, maskCanvas.height)
  }
  cropRect.value = null
  refreshCanvas()
}

function rotate(deg) {
  editState.rotation = (editState.rotation + deg + 360) % 360
  refreshCanvas()
}

function toggleFlip(key) {
  editState[key] = !editState[key]
  refreshCanvas()
}

function selectPreset(presetId) {
  Object.assign(editState, applyPreset(editState, presetId))
  refreshCanvas()
}

function onManualAdjust() {
  editState.preset = 'custom'
  editState.convolution = null
  refreshCanvas()
}

function getCanvasPoint(event) {
  const canvas = canvasRef.value
  const stage = canvas.parentElement
  const stageRect = stage.getBoundingClientRect()
  const scaleX = canvas.width / stageRect.width
  const scaleY = canvas.height / stageRect.height
  return {
    x: (event.clientX - stageRect.left) * scaleX,
    y: (event.clientY - stageRect.top) * scaleY
  }
}

function onWheel(event) {
  if (!imageLoaded.value) return
  const delta = event.deltaY > 0 ? -0.1 : 0.1
  viewScale.value = Math.min(4, Math.max(0.5, viewScale.value + delta))
}

function onPointerDown(event) {
  if (!imageLoaded.value) return
  const point = getCanvasPoint(event)

  if (toolMode.value === 'view' && viewScale.value > 1) {
    isPanning = true
    panStart = { x: event.clientX, y: event.clientY }
    panOrigin = { x: viewPanX.value, y: viewPanY.value }
    return
  }

  if (toolMode.value === 'crop') {
    cropStart.value = point
    cropRect.value = { x: point.x, y: point.y, w: 0, h: 0 }
    return
  }

  if (toolMode.value === 'mosaic' || toolMode.value === 'eraser') {
    isDrawing = true
    applyBrush(point)
  }
}

function onPointerMove(event) {
  if (!imageLoaded.value) return
  const point = getCanvasPoint(event)

  if (isPanning) {
    viewPanX.value = panOrigin.x + (event.clientX - panStart.x)
    viewPanY.value = panOrigin.y + (event.clientY - panStart.y)
    return
  }

  if (toolMode.value === 'crop' && cropStart.value) {
    const start = cropStart.value
    const x = Math.min(start.x, point.x)
    const y = Math.min(start.y, point.y)
    const w = Math.abs(point.x - start.x)
    const h = Math.abs(point.y - start.y)
    cropRect.value = { x, y, w, h }
    return
  }

  if (isDrawing) {
    applyBrush(point)
  }
}

function onPointerUp() {
  isDrawing = false
  isPanning = false
  cropStart.value = null
}

function applyBrush(point) {
  if (!canvasRef.value || !sourceCanvas || !maskCanvas) return
  const outCtx = canvasRef.value.getContext('2d')
  const radius = brushSize.value

  if (toolMode.value === 'mosaic') {
    paintMaskCircle(maskCanvas, point.x, point.y, radius, true)
    pixelateRegion(sourceCanvas, outCtx, point.x, point.y, radius, mosaicBlockSize.value)
  } else if (toolMode.value === 'eraser') {
    paintMaskCircle(maskCanvas, point.x, point.y, radius, false)
    restoreRegion(sourceCanvas, outCtx, point.x, point.y, radius)
  }
}

function clearCrop() {
  cropRect.value = null
  cropStart.value = null
}

async function applyCrop() {
  if (!cropRect.value || !canvasRef.value) return
  const { x, y, w, h } = cropRect.value
  if (w < 10 || h < 10) {
    ElMessage.warning('裁剪区域太小')
    return
  }

  const cropped = document.createElement('canvas')
  cropped.width = Math.floor(w)
  cropped.height = Math.floor(h)
  cropped.getContext('2d').drawImage(canvasRef.value, x, y, w, h, 0, 0, w, h)

  originalImage = await canvasToImage(cropped)
  Object.assign(editState, createDefaultEditState())
  resetLayers()
  imageLoaded.value = true
  refreshCanvas()
  toolMode.value = 'view'
  ElMessage.success('裁剪完成')
}

async function loadImageFromElement(img) {
  originalImage = img
  Object.assign(editState, createDefaultEditState())
  resetLayers()
  imageLoaded.value = true
  await refreshCanvas()
}

function loadImageFromSrc(src) {
  const img = new Image()
  if (!src.startsWith('data:')) {
    img.crossOrigin = 'anonymous'
  }
  img.onload = () => loadImageFromElement(img)
  img.onerror = () => ElMessage.error('图片加载失败')
  img.src = src
}

function loadImage(e) {
  const file = e.target.files[0]
  if (!file) return
  editingPhotoId.value = null
  editingPhotoName.value = file.name
  const reader = new FileReader()
  reader.onload = ev => loadImageFromSrc(ev.target.result)
  reader.readAsDataURL(file)
}

async function loadPhotoById(photoId) {
  try {
    const res = await getPhoto(photoId)
    editingPhotoId.value = res.data.id
    editingPhotoName.value = res.data.originalName
    loadImageFromSrc(`${res.data.url}?t=${Date.now()}`)
  } catch (e) {
    ElMessage.error('加载照片失败')
  }
}

function downloadImage() {
  const canvas = canvasRef.value
  const link = document.createElement('a')
  const baseName = editingPhotoName.value?.replace(/\.[^.]+$/, '') || 'imprint-edited'
  link.download = `${baseName}-edited.png`
  link.href = canvas.toDataURL('image/png')
  link.click()
  ElMessage.success('已下载')
}

function canvasToBlob(canvas) {
  return new Promise(resolve => {
    canvas.toBlob(blob => resolve(blob), 'image/png')
  })
}

async function saveOverwrite() {
  if (!editingPhotoId.value) {
    ElMessage.warning('请从相册选择照片后再覆盖保存')
    return
  }
  try {
    await ElMessageBox.confirm('确定用当前编辑结果覆盖原图吗？此操作不可撤销。', '覆盖保存', {
      type: 'warning',
      confirmButtonText: '覆盖保存',
      cancelButtonText: '取消'
    })
    saving.value = true
    const canvas = canvasRef.value
    const blob = await canvasToBlob(canvas)
    const file = new File([blob], editingPhotoName.value || 'edited.png', { type: 'image/png' })
    await saveEditedPhoto(editingPhotoId.value, file)
    ElMessage.success('已覆盖保存原图')
    Object.assign(editState, resetEditState(editState))
    resetLayers()
    await loadPhotoById(editingPhotoId.value)
  } catch (e) {
    if (e !== 'cancel') {
      throw e
    }
  } finally {
    saving.value = false
  }
}

function backToGallery() {
  router.push('/gallery')
}

watch(toolMode, () => {
  clearCrop()
})

onMounted(() => {
  if (route.query.photoId) {
    loadPhotoById(route.query.photoId)
  }
})

watch(
  () => route.query.photoId,
  photoId => {
    if (photoId) {
      loadPhotoById(photoId)
    }
  }
)
</script>

<style scoped>
.edit-page {
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

.panel-capsule {
  padding: 16px 14px;
  border-radius: 16px;
  background: rgba(18, 18, 28, 0.78);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.28);
}

.edit-page h2 {
  text-align: center;
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 1px;
}

.hint {
  text-align: center;
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: rgba(168, 216, 234, 0.95);
  letter-spacing: 0.5px;
}

.edit-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  justify-content: center;
}

.side-panel h3,
.filter-section h3 {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.92);
  margin-bottom: 12px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.section-gap {
  margin-top: 16px;
}

.side-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 150px;
}

.adjust-panel {
  width: 230px;
}

.edit-btn {
  width: 100%;
  margin-left: 0;
}

.tool-group {
  display: flex;
  flex-wrap: wrap;
}

.tool-tip {
  font-size: 12px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.88);
  line-height: 1.5;
  margin-top: 8px;
  padding: 8px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.slider-item {
  margin-bottom: 12px;
}

.slider-item span {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 4px;
}

.panel-capsule :deep(.el-radio-button__inner) {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.75);
}

.panel-capsule :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: rgba(74, 144, 226, 0.85);
  border-color: #4a90e2;
  color: #fff;
  box-shadow: none;
}

.panel-capsule :deep(.el-slider__runway) {
  background: rgba(255, 255, 255, 0.15);
}

.panel-capsule :deep(.el-slider__bar) {
  background: #4a90e2;
}

.panel-capsule :deep(.el-button) {
  --el-button-bg-color: rgba(255, 255, 255, 0.1);
  --el-button-border-color: rgba(255, 255, 255, 0.2);
  --el-button-text-color: rgba(255, 255, 255, 0.9);
  --el-button-hover-bg-color: rgba(255, 255, 255, 0.18);
  --el-button-hover-border-color: rgba(255, 255, 255, 0.3);
  --el-button-hover-text-color: #fff;
}

.canvas-viewport {
  flex: 1;
  max-width: 640px;
  min-height: 440px;
  max-height: 560px;
  background: repeating-conic-gradient(#eee 0% 25%, #fff 0% 50%) 50% / 20px 20px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: inset 0 0 0 1px #ddd;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.canvas-viewport.is-pan {
  cursor: grab;
}

.canvas-viewport.is-crop {
  cursor: crosshair;
}

.canvas-viewport.is-brush {
  cursor: crosshair;
}

.canvas-stage {
  position: relative;
  transform-origin: center center;
  transition: transform 0.05s linear;
}

.placeholder {
  color: #aaa;
  text-align: center;
  cursor: pointer;
  padding: 60px;
  background: #1a1a2e;
  width: 100%;
  height: 100%;
  min-height: 440px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
}

canvas {
  display: block;
  max-width: 100%;
  max-height: 520px;
}

.crop-box {
  position: absolute;
  border: 2px dashed #4a90e2;
  background: rgba(74, 144, 226, 0.15);
  pointer-events: none;
  box-sizing: border-box;
}

.filter-section {
  margin-top: 24px;
}

.filter-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-chip {
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.85);
  padding: 8px 16px;
  border-radius: 999px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
}

.filter-chip:hover {
  border-color: rgba(110, 181, 255, 0.8);
  color: #fff;
  background: rgba(74, 144, 226, 0.25);
}

.filter-chip.active {
  background: #4a90e2;
  border-color: #4a90e2;
  color: #fff;
  font-weight: 600;
}

.bottom-actions {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 24px;
}
</style>
