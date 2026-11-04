import api from './api';
import type {
  User,
  UserLoginRequest,
  UserLoginResponse,
  UserRegisterRequest,
  UserUpdateRequest,
  ChangePasswordRequest,
} from '@/types/user';

/**
 * 用户注册
 */
export const register = (data: UserRegisterRequest): Promise<User> => {
  return api.post('/api/register', data);
};

/**
 * 用户登录
 */
export const login = (data: UserLoginRequest): Promise<UserLoginResponse> => {
  return api.post('/api/login', data);
};

/**
 * 获取当前用户信息
 */
export const getUserInfo = (): Promise<User> => {
  return api.get('/api/info');
};

/**
 * 更新用户信息
 */
export const updateUserInfo = (data: UserUpdateRequest): Promise<User> => {
  return api.put('/api/info', data);
};

/**
 * 修改密码
 */
export const changePassword = (data: ChangePasswordRequest): Promise<void> => {
  return api.post('/api/change-password', data);
};

export default {
  register,
  login,
  getUserInfo,
  updateUserInfo,
  changePassword,
};
