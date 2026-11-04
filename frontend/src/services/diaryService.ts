import { get, post, put, del } from './api';
import type { Diary, DiaryCreateRequest, DiaryUpdateRequest } from '@/types/diary';

/**
 * 创建日记
 */
export const createDiary = (data: DiaryCreateRequest): Promise<number> => {
  return post('/api/diary', data);
};

/**
 * 获取日记列表（按宠物）
 */
export const listDiaries = (petId?: number, page?: number, size?: number): Promise<Diary[]> => {
  return get('/api/diary', { params: { petId, page, size } });
};

/**
 * 获取单条日记
 */
export const getDiary = (id: number): Promise<Diary> => {
  return get(`/api/diary/${id}`);
};

/**
 * 按日期获取日记
 */
export const getDiaryByDate = (petId: number, date: string): Promise<Diary> => {
  return get(`/api/diary/date/${petId}/${date}`);
};

/**
 * 更新日记
 */
export const updateDiary = (id: number, data: DiaryUpdateRequest): Promise<Diary> => {
  return put(`/api/diary/${id}`, data);
};

/**
 * 删除日记
 */
export const deleteDiary = (id: number): Promise<void> => {
  return del(`/api/diary/${id}`);
};

/**
 * 点赞日记
 */
export const likeDiary = (id: number): Promise<void> => {
  return post(`/api/diary/${id}/like`);
};

/**
 * 获取所有日记
 */
export const getAllDiaries = (page?: number, size?: number): Promise<Diary[]> => {
  return get('/api/diary/all', { params: { page, size } });
};

export default {
  createDiary,
  listDiaries,
  getDiary,
  getDiaryByDate,
  updateDiary,
  deleteDiary,
  likeDiary,
  getAllDiaries,
};
