import { request } from '../utils/request';
import type { AuthUser } from '../utils/auth';

export interface LoginResult {
  token: string;
  tokenType: string;
  expiresIn: number;
  user: AuthUser;
}

export function login(username: string, password: string): Promise<LoginResult> {
  return request<LoginResult>({
    url: '/user/login',
    method: 'POST',
    data: { username, password },
    showLoading: true,
    auth: false,
  });
}

export function getProfile(): Promise<AuthUser> {
  return request<AuthUser>({ url: '/user/info' });
}

export function updateProfile(data: Partial<AuthUser>): Promise<AuthUser> {
  return request<AuthUser>({
    url: '/user/info',
    method: 'PUT',
    data,
    showLoading: true,
  });
}
