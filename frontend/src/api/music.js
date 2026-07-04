import request from './request'

export function getMusicList() {
  return request.get('/api/music')
}

export function uploadMusic(files) {
  const formData = new FormData()
  files.forEach(file => formData.append('files', file))
  return request.post('/api/music/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function deleteMusic(id) {
  return request.delete(`/api/music/${id}`)
}
