import api from './api';
import type { Pet, PetCreateRequest, PetUpdateRequest } from '@/types/pet';

/**
 * 创建宠物
 */
export const createPet = (data: PetCreateRequest): Promise<Pet> => {
  return api.post('/api/pet', data);
};

/**
 * 获取宠物列表
 */
export const listPets = (page?: number, size?: number): Promise<Pet[]> => {
  const params = new URLSearchParams();
  if (page) params.append('page', String(page));
  if (size) params.append('size', String(size));
  const query = params.toString() ? `?${params.toString()}` : '';
  return api.get(`/api/pet${query}`);
};

/**
 * 获取单个宠物
 */
export const getPet = (id: number): Promise<Pet> => {
  return api.get(`/api/pet/${id}`);
};

/**
 * 更新宠物信息
 */
export const updatePet = (id: number, data: PetUpdateRequest): Promise<Pet> => {
  return api.put(`/api/pet/${id}`, data);
};

/**
 * 删除宠物
 */
export const deletePet = (id: number): Promise<void> => {
  return api.delete(`/api/pet/${id}`);
};
