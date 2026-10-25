import request from './request'

// 当前后端登录地址是 /login，不是旧版参考实现的 /auth/login
export const login = (data) => request.post('/login', data)

export const logout = () => request.post('/logout')

// 登录响应只有基础身份，角色与权限由这个接口补齐
export const me = () => request.get('/auth/me')
