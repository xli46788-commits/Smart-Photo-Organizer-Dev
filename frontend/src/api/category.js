import request from './request'

export function getCategories() {
  return request.get('/api/categories')
}

export function createCategory(name, keywords = []) {
  return request.post('/api/categories', { name, keywords })
}

export function updateCategoryKeywords(id, keywords) {
  return request.put(`/api/categories/${id}/keywords`, { keywords })
}

export function deleteCategory(id, deletePhotos) {
  return request.delete(`/api/categories/${id}`, { data: { deletePhotos } })
}
