import { create } from 'zustand';
import Taro from '@tarojs/taro';
import { userService } from '@/services/userService';
import { api } from '@/services/api';
import type { User, UserLoginRequest, UserRegisterRequest } from '@/types/user';

interface AuthStore {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  loading: boolean;
  error: string | null;

  // 方法
  login: (data: UserLoginRequest) => Promise<void>;
  register: (data: UserRegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
  fetchUserInfo: () => Promise<void>;
  setToken: (token: string) => void;
  clearAuth: () => void;
  initAuth: () => void;
}

export const useAuth = create<AuthStore>((set) => ({
  user: null,
  token: null,
  isAuthenticated: false,
  loading: false,
  error: null,

  // 初始化认证状态（从本地存储恢复）
  initAuth: () => {
    try {
      const token = Taro.getStorageSync('token');
      if (token) {
        set({ token, isAuthenticated: true });
        api.setToken(token);
      }
    } catch (error) {
      console.error('Failed to init auth:', error);
    }
  },

  // 登录
  login: async (data: UserLoginRequest) => {
    set({ loading: true, error: null });
    try {
      const response = await userService.login(data);
      const { token, user } = response;

      // 保存 Token 到本地
      Taro.setStorageSync('token', token);
      api.setToken(token);

      set({
        user,
        token,
        isAuthenticated: true,
        loading: false,
      });
    } catch (error: any) {
      set({
        error: error.message || '登录失败',
        loading: false,
      });
      throw error;
    }
  },

  // 注册
  register: async (data: UserRegisterRequest) => {
    set({ loading: true, error: null });
    try {
      await userService.register(data);
      set({ loading: false });
    } catch (error: any) {
      set({
        error: error.message || '注册失败',
        loading: false,
      });
      throw error;
    }
  },

  // 获取用户信息
  fetchUserInfo: async () => {
    set({ loading: true, error: null });
    try {
      const user = await userService.getUserInfo();
      set({ user, loading: false });
    } catch (error: any) {
      set({
        error: error.message || '获取用户信息失败',
        loading: false,
      });
      throw error;
    }
  },

  // 设置 Token
  setToken: (token: string) => {
    Taro.setStorageSync('token', token);
    api.setToken(token);
    set({ token, isAuthenticated: true });
  },

  // 登出
  logout: async () => {
    set({ loading: true });
    try {
      // 清除本地存储
      Taro.removeStorageSync('token');
      Taro.removeStorageSync('user');
      api.clearToken();

      set({
        user: null,
        token: null,
        isAuthenticated: false,
        loading: false,
      });

      // 重定向到登录页
      Taro.navigateTo({ url: '/pages/auth/login' });
    } catch (error) {
      console.error('Logout error:', error);
      set({ loading: false });
    }
  },

  // 清除认证状态
  clearAuth: () => {
    Taro.removeStorageSync('token');
    api.clearToken();
    set({
      user: null,
      token: null,
      isAuthenticated: false,
    });
  },
}));

export default useAuth;
