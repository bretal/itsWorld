// User 相关类型定义

export interface User {
  id: number;
  username: string;
  email: string;
  phone?: string;
  nickName?: string;
  avatarUrl?: string;
  bio?: string;
  gender?: number; // 0=未知, 1=男, 2=女
  region?: string;
  role?: string; // USER / ADMIN
}

export interface UserLoginRequest {
  username: string;
  password: string;
}

export interface UserLoginResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface UserRegisterRequest {
  username: string;
  phone: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export interface UserUpdateRequest {
  nickName?: string;
  email?: string;
  avatarUrl?: string;
  bio?: string;
  gender?: number;
  region?: string;
}

export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
  confirmPassword?: string;
}
