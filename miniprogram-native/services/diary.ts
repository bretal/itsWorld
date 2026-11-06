import { request } from '../utils/request';
import type { PageData } from '../types/page';

export interface Diary {
  id: number;
  userId?: number;
  petId: number;
  diaryDate: string;
  greeting?: string;
  healthScore?: number;
  content?: string;
  photoUrls?: string[];
  likeCount?: number;
  commentCount?: number;
  isPublic?: number;
  createdAt?: string;
  updatedAt?: string;
  /** 后端待补字段：全部 / 饮食 / 健康 / 日常 */
  category?: string;
}

export interface DiaryPayload {
  petId: number;
  diaryDate: string;
  location?: string;
  mood?: string;
  activities?: string[];
  healthScore?: number;
  content?: string;
  photoUrls?: string[];
  isPublic?: number;
  category?: string;
}

export function listDiaries(petId: number): Promise<Diary[]> {
  return request<Diary[]>({ url: '/diary', data: { petId } });
}

export function listDiariesPage(
  petId: number,
  page = 1,
  size = 10,
  category?: string,
): Promise<PageData<Diary>> {
  return request<PageData<Diary>>({
    url: '/diary/page',
    data: { petId, page, size, category },
  });
}

export function getDiary(diaryId: number): Promise<Diary> {
  return request<Diary>({ url: `/diary/${diaryId}` });
}

export function getDiaryByDate(petId: number, date: string): Promise<Diary> {
  return request<Diary>({ url: `/diary/date/${petId}/${date}` });
}

export function createDiary(payload: DiaryPayload): Promise<number> {
  return request<number>({
    url: '/diary',
    method: 'POST',
    data: payload,
    showLoading: true,
  });
}

export function deleteDiary(diaryId: number): Promise<void> {
  return request<void>({
    url: `/diary/${diaryId}`,
    method: 'DELETE',
    showLoading: true,
  });
}

export function likeDiary(diaryId: number): Promise<void> {
  return request<void>({ url: `/diary/${diaryId}/like`, method: 'POST' });
}
