import { post, get, put } from './api';
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
export const register = (data: UserRegisterRequest): Promise<number> => {
  return post('/api/user/register', data);
};

/**
 * 用户登录
 */
export const login = (data: UserLoginRequest): Promise<UserLoginResponse> => {
  return post('/api/user/login', data);
};

/**
 * 获取当前用户信息
 */
export const getUserInfo = (): Promise<User> => {
  return get('/api/user/info');
};

/**
 * 更新用户信息
 */
export const updateUserInfo = (data: UserUpdateRequest): Promise<User> => {
  return put('/api/user/info', data);
};

/**
 * 修改密码
 */
export const changePassword = (data: ChangePasswordRequest): Promise<void> => {
  return post('/api/user/change-password', data);
};

export default {
  register,
  login,
  getUserInfo,
  updateUserInfo,
  changePassword,
};
