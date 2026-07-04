/** 拼图布局：cells 为 [x, y, w, h] 相对比例 0~1 */
export const COLLAGE_LAYOUTS = [
  {
    id: '2-h',
    name: '两图横排',
    slots: 2,
    cells: [[0, 0, 0.5, 1], [0.5, 0, 0.5, 1]]
  },
  {
    id: '2-v',
    name: '两图竖排',
    slots: 2,
    cells: [[0, 0, 1, 0.5], [0, 0.5, 1, 0.5]]
  },
  {
    id: '3-h',
    name: '三图横排',
    slots: 3,
    cells: [[0, 0, 1 / 3, 1], [1 / 3, 0, 1 / 3, 1], [2 / 3, 0, 1 / 3, 1]]
  },
  {
    id: '3-v',
    name: '三图竖排',
    slots: 3,
    cells: [[0, 0, 1, 1 / 3], [0, 1 / 3, 1, 1 / 3], [0, 2 / 3, 1, 1 / 3]]
  },
  {
    id: '4-grid',
    name: '四宫格',
    slots: 4,
    cells: [
      [0, 0, 0.5, 0.5],
      [0.5, 0, 0.5, 0.5],
      [0, 0.5, 0.5, 0.5],
      [0.5, 0.5, 0.5, 0.5]
    ]
  },
  {
    id: '1-2',
    name: '左大右双',
    slots: 3,
    cells: [[0, 0, 0.58, 1], [0.58, 0, 0.42, 0.5], [0.58, 0.5, 0.42, 0.5]]
  },
  {
    id: '2-1',
    name: '上双下大',
    slots: 3,
    cells: [[0, 0, 0.5, 0.42], [0.5, 0, 0.5, 0.42], [0, 0.42, 1, 0.58]]
  },
  {
    id: '6-grid',
    name: '六宫格',
    slots: 6,
    cells: [
      [0, 0, 1 / 3, 0.5],
      [1 / 3, 0, 1 / 3, 0.5],
      [2 / 3, 0, 1 / 3, 0.5],
      [0, 0.5, 1 / 3, 0.5],
      [1 / 3, 0.5, 1 / 3, 0.5],
      [2 / 3, 0.5, 1 / 3, 0.5]
    ]
  }
]

export const OUTPUT_SIZES = [
  { id: '1200x800', label: '横版 1200×800', width: 1200, height: 800 },
  { id: '800x1200', label: '竖版 800×1200', width: 800, height: 1200 },
  { id: '1200x1200', label: '方形 1200×1200', width: 1200, height: 1200 },
  { id: '1600x900', label: '宽屏 1600×900', width: 1600, height: 900 }
]

export function pickDefaultLayout(imageCount) {
  if (imageCount <= 1) return COLLAGE_LAYOUTS[0]
  if (imageCount === 2) return COLLAGE_LAYOUTS.find(l => l.id === '2-h')
  if (imageCount === 3) return COLLAGE_LAYOUTS.find(l => l.id === '3-h')
  if (imageCount === 4) return COLLAGE_LAYOUTS.find(l => l.id === '4-grid')
  if (imageCount <= 6) return COLLAGE_LAYOUTS.find(l => l.id === '6-grid')
  return COLLAGE_LAYOUTS.find(l => l.id === '6-grid')
}

function drawCover(ctx, img, dx, dy, dw, dh) {
  const ir = img.width / img.height
  const r = dw / dh
  let sw
  let sh
  let sx
  let sy
  if (ir > r) {
    sh = img.height
    sw = sh * r
    sx = (img.width - sw) / 2
    sy = 0
  } else {
    sw = img.width
    sh = sw / r
    sx = 0
    sy = (img.height - sh) / 2
  }
  ctx.drawImage(img, sx, sy, sw, sh, dx, dy, dw, dh)
}

export function renderCollage(canvas, images, layout, options = {}) {
  const {
    outputWidth: W,
    outputHeight: H,
    gap = 8,
    background = '#ffffff',
    borderRadius = 0
  } = options

  canvas.width = W
  canvas.height = H
  const ctx = canvas.getContext('2d')
  ctx.fillStyle = background
  ctx.fillRect(0, 0, W, H)

  layout.cells.forEach((cell, index) => {
    const [cx, cy, cw, ch] = cell
    const img = images[index]
    if (!img) return

    const x = Math.round(cx * W + gap / 2)
    const y = Math.round(cy * H + gap / 2)
    const w = Math.round(cw * W - gap)
    const h = Math.round(ch * H - gap)

    ctx.save()
    if (borderRadius > 0) {
      roundRect(ctx, x, y, w, h, borderRadius)
      ctx.clip()
    }
    drawCover(ctx, img, x, y, w, h)
    ctx.restore()
  })
}

function roundRect(ctx, x, y, w, h, r) {
  const radius = Math.min(r, w / 2, h / 2)
  ctx.beginPath()
  ctx.moveTo(x + radius, y)
  ctx.lineTo(x + w - radius, y)
  ctx.quadraticCurveTo(x + w, y, x + w, y + radius)
  ctx.lineTo(x + w, y + h - radius)
  ctx.quadraticCurveTo(x + w, y + h, x + w - radius, y + h)
  ctx.lineTo(x + radius, y + h)
  ctx.quadraticCurveTo(x, y + h, x, y + h - radius)
  ctx.lineTo(x, y + radius)
  ctx.quadraticCurveTo(x, y, x + radius, y)
  ctx.closePath()
}

export function loadImageFromFile(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const img = new Image()
      img.onload = () => resolve({ img, name: file.name, file })
      img.onerror = reject
      img.src = reader.result
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

export function loadImageFromUrl(url, name = 'photo.jpg') {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => resolve({ img, name, file: null })
    img.onerror = reject
    img.src = url
  })
}
