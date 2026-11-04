import { get, post, put, del } from './api';
import type { Pet, PetCreateRequest, PetUpdateRequest } from '@/types/pet';

/**
 * 创建宠物
 */
export const createPet = (data: PetCreateRequest): Promise<number> => {
  return post('/api/pet', data);
};

/**
 * 获取宠物列表
 */
export const listPets = (page?: number, size?: number): Promise<Pet[]> => {
  return get('/api/pet', { params: { page, size } });
};

/**
 * 获取单个宠物
 */
export const getPet = (id: number): Promise<Pet> => {
  return get(`/api/pet/${id}`);
};

/**
 * 更新宠物信息
 */
export const updatePet = (id: number, data: PetUpdateRequest): Promise<Pet> => {
  return put(`/api/pet/${id}`, data);
};

/**
 * 删除宠物
 */
export const deletePet = (id: number): Promise<void> => {
  return del(`/api/pet/${id}`);
};

export default {
  createPet,
  listPets,
  getPet,
  updatePet,
  deletePet,
};
