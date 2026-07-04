import request from './request'

export function register(data) {
  return request.post('/api/auth/register', data)
}

export function login(data) {
  return request.post('/api/auth/login', data)
}

export function getProfile() {
  return request.get('/api/user/profile')
}
