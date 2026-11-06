import { normalizeMinioUrl } from './image';

export interface AuthUser {
  id: number;
  username: string;
  email?: string;
  phone?: string;
  nickName?: string;
  avatarUrl?: string;
  bio?: string;
  gender?: number;
  region?: string;
}

const TOKEN_KEY = 'itsworld_token';
const USER_KEY = 'itsworld_user';

export function getToken(): string {
  return wx.getStorageSync(TOKEN_KEY) || '';
}

export function getUser(): AuthUser | null {
  const user = wx.getStorageSync(USER_KEY) || null;
  if (user && user.avatarUrl) {
    user.avatarUrl = normalizeMinioUrl(user.avatarUrl);
  }
  return user;
}

export function setAuth(token: string, user: AuthUser): void {
  wx.setStorageSync(TOKEN_KEY, token);
  wx.setStorageSync(USER_KEY, user);
  try {
    const app = getApp<IAppOption>();
    if (app) {
      app.globalData.user = user;
    }
  } catch (error) {
    // App 尚未初始化时忽略
  }
}

export function clearAuth(): void {
  wx.removeStorageSync(TOKEN_KEY);
  wx.removeStorageSync(USER_KEY);
  try {
    const app = getApp<IAppOption>();
    if (app) {
      app.globalData.user = null;
    }
  } catch (error) {
    // App 尚未初始化时忽略
  }
}

export function isLoggedIn(): boolean {
  return Boolean(getToken());
}

export function requireLogin(): boolean {
  if (isLoggedIn()) {
    return true;
  }
  wx.reLaunch({ url: '/pages/auth/login' });
  return false;
}
