import axios from 'axios';
import type { AxiosInstance, AxiosRequestConfig } from 'axios';
import Cookies from 'js-cookie';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

// 创建 axios 实例
const api: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
});

// 统一返回格式接口
export interface Result<T = any> {
  code: number;
  message: string;
  data?: T;
  timestamp?: number;
}

// 请求拦截器：自动加 Token
api.interceptors.request.use(
  (config) => {
    const token = Cookies.get('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截器：处理 Result 格式和错误
api.interceptors.response.use(
  (response: any) => {
    const { code, message, data } = response.data as Result;

    // 成功响应
    if (code === 0) {
      return Promise.resolve(data);
    }

    // 业务错误
    const error = new Error(message);
    (error as any).code = code;
    return Promise.reject(error);
  },
  (error) => {
    // 网络错误或 HTTP 错误
    if (error.response) {
      const { status, data } = error.response;

      // Token 过期或无效
      if (status === 401) {
        Cookies.remove('token');
        localStorage.removeItem('user');
        window.location.href = '/login';
        return Promise.reject(new Error('认证过期，请重新登录'));
      }

      // 使用后端返回的 message
      if (data && data.message) {
        return Promise.reject(new Error(data.message));
      }

      return Promise.reject(new Error(`错误: ${status}`));
    }

    // 网络错误
    if (error.message === 'Network Error') {
      return Promise.reject(new Error('网络连接失败'));
    }

    return Promise.reject(error);
  }
);

/**
 * 通用 GET 请求
 */
export const get = <T = any>(url: string, config?: AxiosRequestConfig): Promise<T> => {
  return api.get(url, config) as Promise<T>;
};

/**
 * 通用 POST 请求
 */
export const post = <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
  return api.post(url, data, config) as Promise<T>;
};

/**
 * 通用 PUT 请求
 */
export const put = <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
  return api.put(url, data, config) as Promise<T>;
};

/**
 * 通用 DELETE 请求
 */
export const del = <T = any>(url: string, config?: AxiosRequestConfig): Promise<T> => {
  return api.delete(url, config) as Promise<T>;
};

export default api;
