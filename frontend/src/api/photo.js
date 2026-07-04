import request from './request'

export function uploadPhotos(files) {
  const formData = new FormData()
  files.forEach(file => formData.append('files', file))
  return request.post('/api/photos/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getPhotos(params) {
  return request.get('/api/photos', { params })
}

export function getPhoto(id) {
  return request.get(`/api/photos/${id}`)
}

export function searchPhotos(params) {
  return request.get('/api/photos/search', { params })
}

export function updateCategory(id, category) {
  return request.put(`/api/photos/${id}/category`, { category })
}

export function deletePhoto(id) {
  return request.delete(`/api/photos/${id}`)
}

export function saveEditedPhoto(id, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.put(`/api/photos/${id}/save`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
