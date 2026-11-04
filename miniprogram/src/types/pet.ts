// Pet 相关类型定义

export interface Pet {
  id: number;
  userId: number;
  petName: string;
  avatarUrl?: string;
  petType: string; // dog, cat, rabbit, etc.
  breed?: string;
  gender?: number; // 0=未知, 1=男, 2=女
  birthDate?: string;
  weight?: number;
  color?: string;
  microchipId?: string;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface PetCreateRequest {
  petName: string;
  petType: string;
  breed?: string;
  gender?: number;
  birthDate?: string;
  weight?: number;
  color?: string;
  microchipId?: string;
  description?: string;
}

export interface PetUpdateRequest {
  petName?: string;
  petType?: string;
  breed?: string;
  gender?: number;
  birthDate?: string;
  weight?: number;
  color?: string;
  microchipId?: string;
  description?: string;
  avatarUrl?: string;
}
