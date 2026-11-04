import Taro from '@tarojs/taro';

const API_BASE_URL = process.env.TARO_APP_API_URL || 'http://localhost:8080';

// Result<T> 接口定义
interface Result<T = any> {
  code: number;
  message: string;
  data: T;
}

// 错误码枚举
enum ErrorCode {
  SUCCESS = 0,
  INVALID_REQUEST = 1,
  INVALID_TOKEN = 2,
  NOT_FOUND = 3,
  BUSINESS_ERROR = 4,
  SYSTEM_ERROR = 5,
}

class TaroAPI {
  private baseURL: string = API_BASE_URL;
  private token: string = '';

  constructor() {
    this.loadToken();
  }

  private loadToken() {
    try {
      const tokenData = Taro.getStorageSync('token');
      if (tokenData) {
        this.token = tokenData;
      }
    } catch (error) {
      console.error('Failed to load token:', error);
    }
  }

  public setToken(token: string) {
    this.token = token;
    try {
      Taro.setStorageSync('token', token);
    } catch (error) {
      console.error('Failed to save token:', error);
    }
  }

  public getToken(): string {
    return this.token;
  }

  public clearToken() {
    this.token = '';
    try {
      Taro.removeStorageSync('token');
    } catch (error) {
      console.error('Failed to clear token:', error);
    }
  }

  public async request<T = any>(
    url: string,
    options: {
      method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
      data?: any;
      headers?: Record<string, string>;
    } = {}
  ): Promise<T> {
    const { method = 'GET', data, headers = {} } = options;

    // 添加认证 Token
    const finalHeaders: Record<string, string> = {
      'Content-Type': 'application/json',
      ...headers,
    };

    if (this.token) {
      finalHeaders['Authorization'] = `Bearer ${this.token}`;
    }

    try {
      const response = await Taro.request({
        url: `${this.baseURL}${url}`,
        method: method as any,
        data,
        header: finalHeaders,
      });

      const result = response.data as Result<T>;

      // 处理响应
      if (result.code === ErrorCode.SUCCESS) {
        return result.data;
      } else if (result.code === ErrorCode.INVALID_TOKEN) {
        // Token 过期，清除并重定向到登录
        this.clearToken();
        Taro.redirectTo({ url: '/pages/auth/login' });
        throw new Error('认证已过期，请重新登录');
      } else {
        throw new Error(result.message || '请求失败');
      }
    } catch (error: any) {
      console.error(`API Error [${method} ${url}]:`, error);
      throw error;
    }
  }

  public get<T = any>(url: string, options?: any): Promise<T> {
    return this.request<T>(url, { ...options, method: 'GET' });
  }

  public post<T = any>(url: string, data?: any, options?: any): Promise<T> {
    return this.request<T>(url, { ...options, method: 'POST', data });
  }

  public put<T = any>(url: string, data?: any, options?: any): Promise<T> {
    return this.request<T>(url, { ...options, method: 'PUT', data });
  }

  public delete<T = any>(url: string, options?: any): Promise<T> {
    return this.request<T>(url, { ...options, method: 'DELETE' });
  }

  public patch<T = any>(url: string, data?: any, options?: any): Promise<T> {
    return this.request<T>(url, { ...options, method: 'PATCH', data });
  }
}

const api = new TaroAPI();

export { api, ErrorCode, Result };
export default api;
