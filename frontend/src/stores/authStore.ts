import { create } from 'zustand';
import Cookies from 'js-cookie';
import * as userService from '@/services/userService';
import type { User } from '@/types/user';

interface AuthStore {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  loading: boolean;
  error: string | null;

  // Actions
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, phone: string, email: string, password: string, confirmPassword: string) => Promise<void>;
  logout: () => void;
  setToken: (token: string) => void;
  clearAuth: () => void;
  setUser: (user: User | null) => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  fetchUserInfo: () => Promise<void>;
}

const COOKIE_TOKEN_KEY = 'token';
const STORAGE_USER_KEY = 'user';

export const useAuthStore = create<AuthStore>((set) => ({
  user: localStorage.getItem(STORAGE_USER_KEY)
    ? JSON.parse(localStorage.getItem(STORAGE_USER_KEY)!)
    : null,
  token: Cookies.get(COOKIE_TOKEN_KEY) || null,
  isAuthenticated: !!Cookies.get(COOKIE_TOKEN_KEY),
  loading: false,
  error: null,

  login: async (username: string, password: string) => {
    try {
      set({ loading: true, error: null });
      const response = await userService.login({ username, password });

      // 保存 Token
      Cookies.set(COOKIE_TOKEN_KEY, response.token, {
        expires: response.expiresIn / 86400, // 转换为天数
      });

      // 保存用户信息
      localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(response.user));

      set({
        token: response.token,
        user: response.user,
        isAuthenticated: true,
        loading: false,
      });
    } catch (error: any) {
      const errorMessage = error.message || '登录失败';
      set({ error: errorMessage, loading: false });
      throw error;
    }
  },

  register: async (username: string, phone: string, email: string, password: string, confirmPassword: string) => {
    try {
      set({ loading: true, error: null });
      await userService.register({ username, phone, email, password, confirmPassword });
      set({ loading: false });
    } catch (error: any) {
      const errorMessage = error.message || '注册失败';
      set({ error: errorMessage, loading: false });
      throw error;
    }
  },

  logout: () => {
    Cookies.remove(COOKIE_TOKEN_KEY);
    localStorage.removeItem(STORAGE_USER_KEY);
    set({
      user: null,
      token: null,
      isAuthenticated: false,
      error: null,
    });
  },

  setToken: (token: string) => {
    Cookies.set(COOKIE_TOKEN_KEY, token);
    set({ token, isAuthenticated: true });
  },

  clearAuth: () => {
    Cookies.remove(COOKIE_TOKEN_KEY);
    localStorage.removeItem(STORAGE_USER_KEY);
    set({ user: null, token: null, isAuthenticated: false });
  },

  setUser: (user: User | null) => {
    if (user) {
      localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(user));
    } else {
      localStorage.removeItem(STORAGE_USER_KEY);
    }
    set({ user });
  },

  setLoading: (loading: boolean) => {
    set({ loading });
  },

  setError: (error: string | null) => {
    set({ error });
  },

  fetchUserInfo: async () => {
    try {
      set({ loading: true });
      const user = await userService.getUserInfo();
      set({ user, loading: false });
      localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(user));
    } catch (error: any) {
      set({ loading: false, error: error.message });
    }
  },
}));
