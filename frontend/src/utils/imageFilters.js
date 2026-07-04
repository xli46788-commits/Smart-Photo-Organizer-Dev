const SHARPEN_KERNEL = [
  0, -1, 0,
  -1, 5, -1,
  0, -1, 0
]

const EMBOSS_KERNEL = [
  -2, -1, 0,
  -1, 1, 1,
  0, 1, 2
]

export const FILTER_PRESETS = [
  { id: 'normal', name: '原图', css: '', brightness: 0, contrast: 0, saturation: 100, blur: 0, convolution: null },
  { id: 'grayscale', name: '黑白', css: 'grayscale(100%)', brightness: 0, contrast: 0, saturation: 100, blur: 0, convolution: null },
  { id: 'sepia', name: '复古', css: 'sepia(75%)', brightness: 5, contrast: 0, saturation: 100, blur: 0, convolution: null },
  { id: 'warm', name: '暖色', css: 'sepia(35%)', brightness: 8, contrast: 5, saturation: 110, blur: 0, convolution: null },
  { id: 'cool', name: '冷色', css: 'hue-rotate(15deg)', brightness: 0, contrast: 5, saturation: 90, blur: 0, convolution: null },
  { id: 'vivid', name: '鲜艳', css: 'saturate(160%)', brightness: 5, contrast: 10, saturation: 160, blur: 0, convolution: null },
  { id: 'soft', name: '柔和', css: '', brightness: 10, contrast: -10, saturation: 85, blur: 0, convolution: null },
  { id: 'muted', name: '低饱和', css: 'saturate(45%)', brightness: 0, contrast: 0, saturation: 45, blur: 0, convolution: null },
  { id: 'blur', name: '模糊', css: '', brightness: 0, contrast: 0, saturation: 100, blur: 4, convolution: null },
  { id: 'sharpen', name: '锐化', css: '', brightness: 0, contrast: 5, saturation: 100, blur: 0, convolution: 'sharpen' },
  { id: 'emboss', name: '浮雕', css: '', brightness: 0, contrast: 0, saturation: 100, blur: 0, convolution: 'emboss' },
  { id: 'invert', name: '负片', css: 'invert(100%)', brightness: 0, contrast: 0, saturation: 100, blur: 0, convolution: null },
  { id: 'dramatic', name: '戏剧', css: 'contrast(130%) saturate(120%)', brightness: -5, contrast: 20, saturation: 120, blur: 0, convolution: null },
  { id: 'fade', name: '褪色', css: 'brightness(110%) contrast(85%)', brightness: 10, contrast: -15, saturation: 70, blur: 0, convolution: null }
]

function buildCssFilter(state) {
  const parts = []
  if (state.css) parts.push(state.css)
  parts.push(`brightness(${100 + state.brightness}%)`)
  parts.push(`contrast(${100 + state.contrast}%)`)
  parts.push(`saturate(${state.saturation}%)`)
  if (state.blur > 0) parts.push(`blur(${state.blur}px)`)
  return parts.join(' ').trim()
}

function applyConvolution(imageData, kernel) {
  const { width, height, data } = imageData
  const output = new Uint8ClampedArray(data.length)
  const getPixel = (x, y, channel) => {
    const px = Math.min(width - 1, Math.max(0, x))
    const py = Math.min(height - 1, Math.max(0, y))
    return data[(py * width + px) * 4 + channel]
  }

  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      for (let c = 0; c < 3; c++) {
        let sum = 0
        for (let ky = -1; ky <= 1; ky++) {
          for (let kx = -1; kx <= 1; kx++) {
            const weight = kernel[(ky + 1) * 3 + (kx + 1)]
            sum += getPixel(x + kx, y + ky, c) * weight
          }
        }
        const idx = (y * width + x) * 4 + c
        output[idx] = Math.min(255, Math.max(0, sum))
      }
      output[(y * width + x) * 4 + 3] = data[(y * width + x) * 4 + 3]
    }
  }
  return new ImageData(output, width, height)
}

export function renderEditedImage(canvas, image, state) {
  const w = image.width
  const h = image.height
  const swap = state.rotation % 180 !== 0
  const canvasW = swap ? h : w
  const canvasH = swap ? w : h

  canvas.width = canvasW
  canvas.height = canvasH
  const ctx = canvas.getContext('2d')

  const temp = document.createElement('canvas')
  temp.width = canvasW
  temp.height = canvasH
  const tempCtx = temp.getContext('2d')

  tempCtx.save()
  tempCtx.translate(canvasW / 2, canvasH / 2)
  tempCtx.rotate((state.rotation * Math.PI) / 180)
  tempCtx.scale(state.flipH ? -1 : 1, state.flipV ? -1 : 1)
  tempCtx.drawImage(image, -w / 2, -h / 2, w, h)
  tempCtx.restore()

  ctx.clearRect(0, 0, canvasW, canvasH)
  ctx.filter = buildCssFilter(state)
  ctx.drawImage(temp, 0, 0)
  ctx.filter = 'none'

  if (state.convolution) {
    const imageData = ctx.getImageData(0, 0, canvasW, canvasH)
    const kernel = state.convolution === 'emboss' ? EMBOSS_KERNEL : SHARPEN_KERNEL
    ctx.putImageData(applyConvolution(imageData, kernel), 0, 0)
  }
}

export function createDefaultEditState() {
  return {
    rotation: 0,
    flipH: false,
    flipV: false,
    preset: 'normal',
    css: '',
    brightness: 0,
    contrast: 0,
    saturation: 100,
    blur: 0,
    convolution: null
  }
}

export function applyPreset(state, presetId) {
  const preset = FILTER_PRESETS.find(item => item.id === presetId) || FILTER_PRESETS[0]
  return {
    ...state,
    preset: preset.id,
    css: preset.css,
    brightness: preset.brightness,
    contrast: preset.contrast,
    saturation: preset.saturation,
    blur: preset.blur,
    convolution: preset.convolution
  }
}

export function resetEditState(state) {
  return {
    ...createDefaultEditState(),
    rotation: state.rotation,
    flipH: state.flipH,
    flipV: state.flipV
  }
}

export function renderToOffscreenCanvas(image, state) {
  const canvas = document.createElement('canvas')
  renderEditedImage(canvas, image, state)
  return canvas
}

export function createMaskCanvas(width, height) {
  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  return canvas
}

export function paintMaskCircle(maskCanvas, x, y, radius, value) {
  const ctx = maskCanvas.getContext('2d')
  ctx.fillStyle = value ? '#fff' : '#000'
  ctx.beginPath()
  ctx.arc(x, y, radius, 0, Math.PI * 2)
  ctx.fill()
}

export function pixelateRegion(sourceCanvas, targetCtx, x, y, radius, blockSize) {
  const w = sourceCanvas.width
  const h = sourceCanvas.height
  const x0 = Math.max(0, Math.floor(x - radius))
  const y0 = Math.max(0, Math.floor(y - radius))
  const x1 = Math.min(w, Math.ceil(x + radius))
  const y1 = Math.min(h, Math.ceil(y + radius))
  const rw = x1 - x0
  const rh = y1 - y0
  if (rw <= 0 || rh <= 0) return

  const smallW = Math.max(1, Math.floor(rw / blockSize))
  const smallH = Math.max(1, Math.floor(rh / blockSize))
  const small = document.createElement('canvas')
  small.width = smallW
  small.height = smallH
  const sCtx = small.getContext('2d')
  sCtx.drawImage(sourceCanvas, x0, y0, rw, rh, 0, 0, smallW, smallH)

  targetCtx.save()
  targetCtx.beginPath()
  targetCtx.arc(x, y, radius, 0, Math.PI * 2)
  targetCtx.clip()
  targetCtx.imageSmoothingEnabled = false
  targetCtx.drawImage(small, 0, 0, smallW, smallH, x0, y0, rw, rh)
  targetCtx.restore()
}

export function restoreRegion(sourceCanvas, targetCtx, x, y, radius) {
  const w = sourceCanvas.width
  const h = sourceCanvas.height
  const x0 = Math.max(0, Math.floor(x - radius))
  const y0 = Math.max(0, Math.floor(y - radius))
  const rw = Math.min(w, Math.ceil(x + radius)) - x0
  const rh = Math.min(h, Math.ceil(y + radius)) - y0
  if (rw <= 0 || rh <= 0) return

  targetCtx.save()
  targetCtx.beginPath()
  targetCtx.arc(x, y, radius, 0, Math.PI * 2)
  targetCtx.clip()
  targetCtx.drawImage(sourceCanvas, x0, y0, rw, rh, x0, y0, rw, rh)
  targetCtx.restore()
}

export function compositeWithMosaic(sourceCanvas, maskCanvas, outputCanvas, blockSize = 12) {
  const w = sourceCanvas.width
  const h = sourceCanvas.height
  outputCanvas.width = w
  outputCanvas.height = h

  const outCtx = outputCanvas.getContext('2d')
  outCtx.clearRect(0, 0, w, h)
  outCtx.drawImage(sourceCanvas, 0, 0)

  const pixelated = document.createElement('canvas')
  pixelated.width = w
  pixelated.height = h
  const pCtx = pixelated.getContext('2d')
  const sw = Math.max(1, Math.floor(w / blockSize))
  const sh = Math.max(1, Math.floor(h / blockSize))
  const tiny = document.createElement('canvas')
  tiny.width = sw
  tiny.height = sh
  tiny.getContext('2d').drawImage(sourceCanvas, 0, 0, sw, sh)
  pCtx.imageSmoothingEnabled = false
  pCtx.drawImage(tiny, 0, 0, sw, sh, 0, 0, w, h)

  const temp = document.createElement('canvas')
  temp.width = w
  temp.height = h
  const tCtx = temp.getContext('2d')
  tCtx.drawImage(pixelated, 0, 0)
  tCtx.globalCompositeOperation = 'destination-in'
  tCtx.drawImage(maskCanvas, 0, 0)

  outCtx.drawImage(temp, 0, 0)
}

export function canvasToImage(canvas) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve(img)
    img.onerror = reject
    img.src = canvas.toDataURL('image/png')
  })
}
