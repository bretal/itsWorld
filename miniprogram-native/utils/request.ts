import { ENV } from '../config/env';
import { clearAuth, getToken } from './auth';

export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
  timestamp?: number;
}

interface RequestOptions {
  url: string;
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  data?: Record<string, any>;
  showLoading?: boolean;
  auth?: boolean;
}

let redirectingToLogin = false;

/**
 * 移除 undefined / null 查询参数。
 * wx.request 会把 undefined 序列化成字符串 "undefined"，导致后端误当成真实筛选值。
 */
function sanitizeData(data?: Record<string, any>): Record<string, any> | undefined {
  if (!data) {
    return data;
  }
  const result: Record<string, any> = {};
  Object.keys(data).forEach((key) => {
    const value = data[key];
    if (value !== undefined && value !== null) {
      result[key] = value;
    }
  });
  return result;
}

function handleUnauthorized(): void {
  clearAuth();
  if (redirectingToLogin) {
    return;
  }
  redirectingToLogin = true;
  wx.reLaunch({
    url: '/pages/auth/login',
    complete: () => {
      setTimeout(() => {
        redirectingToLogin = false;
      }, 500);
    },
  });
}

export function request<T>(options: RequestOptions): Promise<T> {
  const token = getToken();

  return new Promise<T>((resolve, reject) => {
    if (options.showLoading) {
      wx.showLoading({ title: '加载中', mask: true });
    }

    wx.request({
      url: `${ENV.baseUrl}${options.url}`,
      method: options.method || 'GET',
      data: sanitizeData(options.data),
      timeout: 15000,
      header: {
        'Content-Type': 'application/json',
        ...(token && options.auth !== false
          ? { Authorization: `Bearer ${token}` }
          : {}),
      },
      success: (response: any) => {
        const body = response.data as ApiResult<T>;

        if (response.statusCode === 401 || body?.code === 401) {
          handleUnauthorized();
          reject(new Error('登录已过期'));
          return;
        }

        if (body && body.code === 0) {
          resolve(body.data);
          return;
        }

        const message = body?.message || '请求失败';
        wx.showToast({ title: message, icon: 'none' });
        reject(new Error(message));
      },
      fail: () => {
        wx.showToast({ title: '网络异常，请稍后重试', icon: 'none' });
        reject(new Error('NETWORK_ERROR'));
      },
      complete: () => {
        if (options.showLoading) {
          wx.hideLoading();
        }
      },
    });
  });
}
