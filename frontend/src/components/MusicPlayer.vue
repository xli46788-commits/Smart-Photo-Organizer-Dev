<template>
  <div class="music-player" :class="{ collapsed: !expanded, docked: isDocked }">
    <input ref="fileInput" type="file" accept="audio/*" multiple hidden @change="onFileSelect" />

    <button
      v-if="isDocked"
      class="dock-tab"
      title="展开音乐播放器"
      @click="undockPlayer"
    >
      <span class="dock-tab-icon">♪</span>
      <span class="dock-tab-label">展开</span>
    </button>

    <template v-else>
    <div v-if="!expanded" class="mini-bar">
      <button class="fab-btn main" title="展开播放器" @click="expanded = true">♪</button>
      <button class="fab-btn" title="上传音乐" @click="pickMusic">＋</button>
      <button
        v-if="hasTrack"
        class="fab-btn"
        :title="playing ? '暂停' : '播放'"
        @click="togglePlay"
      >
        {{ playing ? '❚❚' : '▶' }}
      </button>
      <span v-if="hasTrack" class="mini-name" :title="trackName">{{ trackName }}</span>
      <button class="fab-btn dock-btn" title="隐藏到侧边" @click="dockPlayer">›</button>
    </div>

    <div v-else class="player-body">
      <button class="close-btn" title="收起" @click="expanded = false">−</button>
      <button class="dock-side-btn" title="隐藏到侧边" @click="dockPlayer">隐藏 ›</button>

      <div class="playlist-header">
        <span class="playlist-title">我的音乐（{{ tracks.length }}）</span>
        <el-button size="small" :loading="uploading" @click="pickMusic">上传音乐</el-button>
      </div>

      <div v-if="tracks.length" class="playlist">
        <div
          v-for="track in tracks"
          :key="track.id"
          class="track-item"
          :class="{ active: currentTrackId === track.id }"
          @click="selectTrack(track)"
        >
          <span class="track-title" :title="track.originalName">{{ track.originalName }}</span>
          <button class="delete-track" title="删除" @click.stop="removeTrack(track)">×</button>
        </div>
      </div>
      <p v-else class="empty-tip">暂无音乐，点击「上传音乐」添加</p>

      <div class="track-info">
        <span class="track-name" :title="trackName">正在播放：{{ trackName }}</span>
      </div>

      <div class="controls">
        <el-button circle :disabled="!hasTrack" title="上一首" @click="playPrevTrack">
          <el-icon><DArrowLeft /></el-icon>
        </el-button>
        <el-button circle :disabled="!hasTrack" @click="togglePlay">
          <el-icon><component :is="playing ? VideoPause : VideoPlay" /></el-icon>
        </el-button>
        <el-button circle :disabled="!hasTrack" @click="stop">
          <el-icon><RefreshLeft /></el-icon>
        </el-button>
        <el-button circle :disabled="!hasTrack" title="下一首" @click="playNextTrack">
          <el-icon><DArrowRight /></el-icon>
        </el-button>
        <el-radio-group
          v-model="loopMode"
          size="small"
          :disabled="!hasTrack"
          class="loop-mode-group"
        >
          <el-radio-button value="single">单曲循环</el-radio-button>
          <el-radio-button value="list">列表循环</el-radio-button>
        </el-radio-group>
      </div>

      <div class="autoplay-row">
        <el-checkbox v-model="sessionAutoplayOff" @change="onSessionAutoplayChange">
          关闭本次登录的自动播放
        </el-checkbox>
      </div>

      <div class="progress-row">
        <span class="time">{{ formatTime(currentTime) }}</span>
        <el-slider
          v-model="progress"
          :min="0"
          :max="100"
          :step="0.1"
          :disabled="!hasTrack"
          @input="onSeek"
        />
        <span class="time">{{ formatTime(duration) }}</span>
      </div>

      <div class="volume-row">
        <span>音量</span>
        <el-slider v-model="volume" :min="0" :max="100" @input="onVolumeChange" />
      </div>
    </div>
    </template>

    <audio
      ref="audioRef"
      :src="audioUrl"
      :loop="loopMode === 'single'"
      @timeupdate="onTimeUpdate"
      @loadedmetadata="onLoaded"
      @ended="onEnded"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { VideoPlay, VideoPause, RefreshLeft, DArrowLeft, DArrowRight } from '@element-plus/icons-vue'
import { getMusicList, uploadMusic, deleteMusic } from '../api/music'

const fileInput = ref(null)
const audioRef = ref(null)
const audioUrl = ref('')
const trackName = ref('未选择音乐')
const currentTrackId = ref(null)
const tracks = ref([])
const uploading = ref(false)
const playing = ref(false)
const storedLoopMode = localStorage.getItem('musicLoopMode')
const loopMode = ref(storedLoopMode === 'single' ? 'single' : 'list')
const sessionAutoplayOff = ref(sessionStorage.getItem('musicSessionAutoplayOff') === '1')
const expanded = ref(false)
const isDocked = ref(false)
const volume = ref(Number(localStorage.getItem('bgmVolume') || 60))
const currentTime = ref(0)
const duration = ref(0)
const progress = ref(0)
const hasTrack = ref(false)
const pendingAutoplay = ref(false)
let playbackStarting = false

function pickMusic() {
  fileInput.value?.click()
}

async function loadTracks() {
  if (!localStorage.getItem('token')) return
  try {
    const res = await getMusicList()
    tracks.value = res.data || []
    if (!tracks.value.length) {
      hasTrack.value = false
      trackName.value = '未选择音乐'
      audioUrl.value = ''
      currentTrackId.value = null
      return
    }
    const lastId = localStorage.getItem('lastMusicId')
    const target = tracks.value.find(t => String(t.id) === lastId) || tracks.value[0]
    selectTrack(target, shouldAutoplayOnLoad())
  } catch {
    tracks.value = []
  }
}

async function onFileSelect(e) {
  const files = Array.from(e.target.files || [])
  if (!files.length) return

  uploading.value = true
  try {
    const res = await uploadMusic(files)
    ElMessage.success(`成功上传 ${res.data.uploaded} 首音乐`)
    await loadTracks()
    const uploaded = res.data.tracks
    if (uploaded?.length) {
      selectTrack(uploaded[0])
    }
  } finally {
    uploading.value = false
    e.target.value = ''
  }
}

function selectTrack(track, autoplay = true) {
  if (!track) return

  const isSameTrack = currentTrackId.value === track.id && audioUrl.value === track.url

  if (audioRef.value) {
    audioRef.value.pause()
  }
  playing.value = false
  pendingAutoplay.value = autoplay

  if (isSameTrack) {
    if (audioRef.value) {
      audioRef.value.currentTime = 0
    }
    currentTime.value = 0
    progress.value = 0
  } else {
    currentTime.value = 0
    progress.value = 0
    audioUrl.value = track.url
  }

  trackName.value = track.originalName
  currentTrackId.value = track.id
  hasTrack.value = true
  localStorage.setItem('lastMusicId', String(track.id))

  if (autoplay) {
    nextTick(() => startPlayback())
  } else {
    pendingAutoplay.value = false
  }
}

function waitForCanPlay(audio) {
  if (audio.readyState >= HTMLMediaElement.HAVE_FUTURE_DATA) {
    return Promise.resolve()
  }
  return new Promise((resolve, reject) => {
    const onReady = () => {
      cleanup()
      resolve()
    }
    const onError = () => {
      cleanup()
      reject(new Error('load failed'))
    }
    const cleanup = () => {
      audio.removeEventListener('canplay', onReady)
      audio.removeEventListener('error', onError)
    }
    audio.addEventListener('canplay', onReady, { once: true })
    audio.addEventListener('error', onError, { once: true })
  })
}

async function startPlayback() {
  const audio = audioRef.value
  if (!audio || !audioUrl.value || !pendingAutoplay.value || playbackStarting) return

  playbackStarting = true
  try {
    audio.volume = volume.value / 100
    if (audio.readyState < HTMLMediaElement.HAVE_FUTURE_DATA) {
      await waitForCanPlay(audio)
    }
    await audio.play()
    playing.value = true
    pendingAutoplay.value = false
  } catch {
    pendingAutoplay.value = false
  } finally {
    playbackStarting = false
  }
}

async function removeTrack(track) {
  try {
    await ElMessageBox.confirm(`确定删除「${track.originalName}」吗？`, '删除音乐', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteMusic(track.id)
    ElMessage.success('已删除')
    if (currentTrackId.value === track.id) {
      stopInternal()
      audioUrl.value = ''
      hasTrack.value = false
      trackName.value = '未选择音乐'
      currentTrackId.value = null
    }
    await loadTracks()
  } catch (err) {
    if (err !== 'cancel') throw err
  }
}

function togglePlay() {
  if (!hasTrack.value || !audioRef.value) return
  if (playing.value) {
    audioRef.value.pause()
    playing.value = false
  } else {
    audioRef.value.play().then(() => {
      playing.value = true
    }).catch(() => {
      ElMessage.error('播放失败，请重试')
    })
  }
}

function stopInternal() {
  if (!audioRef.value) return
  audioRef.value.pause()
  audioRef.value.currentTime = 0
  playing.value = false
  currentTime.value = 0
  progress.value = 0
}

function stop() {
  stopInternal()
}

function onTimeUpdate() {
  if (!audioRef.value || !duration.value) return
  currentTime.value = audioRef.value.currentTime
  progress.value = (currentTime.value / duration.value) * 100
}

function onLoaded() {
  if (!audioRef.value) return
  duration.value = audioRef.value.duration || 0
  audioRef.value.volume = volume.value / 100
  if (pendingAutoplay.value) {
    startPlayback()
  }
}

function onSeek(val) {
  if (!audioRef.value || !duration.value) return
  audioRef.value.currentTime = (val / 100) * duration.value
}

function onVolumeChange(val) {
  localStorage.setItem('bgmVolume', String(val))
  if (audioRef.value) {
    audioRef.value.volume = val / 100
  }
}

function shouldAutoplayOnLoad() {
  return !sessionAutoplayOff.value
}

function onSessionAutoplayChange(checked) {
  if (checked) {
    sessionStorage.setItem('musicSessionAutoplayOff', '1')
  } else {
    sessionStorage.removeItem('musicSessionAutoplayOff')
  }
}

function dockPlayer() {
  expanded.value = false
  isDocked.value = true
}

function undockPlayer() {
  isDocked.value = false
}

function playPrevTrack() {
  if (!tracks.value.length) return
  const idx = tracks.value.findIndex(t => t.id === currentTrackId.value)
  const prev = idx > 0
    ? tracks.value[idx - 1]
    : tracks.value[tracks.value.length - 1]
  if (prev) selectTrack(prev, true)
}

function playNextTrack() {
  if (!tracks.value.length) return
  const idx = tracks.value.findIndex(t => t.id === currentTrackId.value)
  const next = idx >= 0 && idx < tracks.value.length - 1
    ? tracks.value[idx + 1]
    : tracks.value[0]
  if (next) selectTrack(next, true)
}

function onEnded() {
  if (loopMode.value === 'single') return
  playNextTrack()
}

function formatTime(sec) {
  if (!sec || !Number.isFinite(sec)) return '0:00'
  const m = Math.floor(sec / 60)
  const s = Math.floor(sec % 60)
  return `${m}:${String(s).padStart(2, '0')}`
}

watch(audioUrl, () => {
  if (audioRef.value) {
    audioRef.value.volume = volume.value / 100
  }
})

watch(loopMode, (mode) => {
  localStorage.setItem('musicLoopMode', mode)
})

onMounted(loadTracks)

onBeforeUnmount(() => {
  stopInternal()
})
</script>

<style scoped>
.music-player {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 1000;
  background: rgba(18, 18, 28, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  color: #fff;
  transition: right 0.35s ease, bottom 0.35s ease;
}

.music-player.docked {
  right: 0;
  bottom: 50%;
  transform: translateY(50%);
  background: transparent;
  border: none;
  box-shadow: none;
  backdrop-filter: none;
}

.dock-tab {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 32px;
  padding: 16px 4px;
  border: none;
  border-radius: 999px 0 0 999px;
  background: rgba(18, 18, 28, 0.94);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-right: none;
  color: #a8d8ea;
  cursor: pointer;
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.35);
  transition: background 0.2s, width 0.2s;
}

.dock-tab:hover {
  background: rgba(28, 28, 42, 0.98);
  width: 36px;
}

.dock-tab-icon {
  font-size: 16px;
  color: #4a90e2;
}

.dock-tab-label {
  writing-mode: vertical-rl;
  font-size: 11px;
  letter-spacing: 2px;
  color: rgba(255, 255, 255, 0.7);
}

.fab-btn.dock-btn {
  font-size: 18px;
  line-height: 1;
}

.dock-side-btn {
  position: absolute;
  top: 8px;
  right: 40px;
  padding: 4px 10px;
  border: none;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.75);
  cursor: pointer;
  font-size: 11px;
  transition: background 0.2s, color 0.2s;
}

.dock-side-btn:hover {
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
}

.music-player.collapsed {
  background: transparent;
  border: none;
  box-shadow: none;
  backdrop-filter: none;
}

.mini-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(18, 18, 28, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 999px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.fab-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
  color: #a8d8ea;
  cursor: pointer;
  font-size: 14px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.fab-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.fab-btn.main {
  background: #4a90e2;
  color: #fff;
  font-size: 16px;
}

.fab-btn.main:hover {
  background: #3a7bc8;
}

.mini-name {
  max-width: 120px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.75);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.player-body {
  position: relative;
  padding: 16px;
  min-width: 360px;
  max-width: 420px;
}

.close-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
}

.playlist-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  padding-right: 28px;
}

.playlist-title {
  font-size: 13px;
  font-weight: 600;
  color: rgba(168, 216, 234, 0.95);
}

.playlist {
  max-height: 140px;
  overflow-y: auto;
  margin-bottom: 12px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.track-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  transition: background 0.15s;
}

.track-item:hover {
  background: rgba(255, 255, 255, 0.08);
}

.track-item.active {
  background: rgba(74, 144, 226, 0.35);
}

.track-title {
  flex: 1;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.delete-track {
  width: 20px;
  height: 20px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  flex-shrink: 0;
}

.delete-track:hover {
  background: rgba(245, 108, 108, 0.5);
  color: #fff;
}

.empty-tip {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 12px;
  text-align: center;
}

.track-info {
  margin-bottom: 10px;
}

.track-name {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.75);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
}

.controls {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.loop-mode-group :deep(.el-radio-button__inner) {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.75);
  padding: 5px 10px;
}

.loop-mode-group :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: rgba(74, 144, 226, 0.45);
  border-color: rgba(74, 144, 226, 0.6);
  color: #fff;
  box-shadow: none;
}

.autoplay-row {
  margin-bottom: 12px;
}

.autoplay-row :deep(.el-checkbox__label) {
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
}

.autoplay-row :deep(.el-checkbox__inner) {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.25);
}

.progress-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.progress-row :deep(.el-slider__runway) {
  background: rgba(255, 255, 255, 0.15);
}

.progress-row :deep(.el-slider__bar) {
  background: #4a90e2;
}

.progress-row .el-slider {
  flex: 1;
}

.time {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.55);
  min-width: 32px;
}

.volume-row {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.65);
}

.volume-row :deep(.el-slider__runway) {
  background: rgba(255, 255, 255, 0.15);
}

.volume-row :deep(.el-slider__bar) {
  background: #4a90e2;
}

.volume-row .el-slider {
  flex: 1;
}

.controls :deep(.el-button) {
  --el-button-bg-color: rgba(255, 255, 255, 0.1);
  --el-button-border-color: rgba(255, 255, 255, 0.15);
  --el-button-text-color: #fff;
}

.playlist-header :deep(.el-button) {
  --el-button-bg-color: rgba(74, 144, 226, 0.35);
  --el-button-border-color: rgba(74, 144, 226, 0.5);
  --el-button-text-color: #fff;
}
</style>
