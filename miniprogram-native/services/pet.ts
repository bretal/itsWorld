import { request } from '../utils/request';
import type { PageData } from '../types/page';

export interface Pet {
  id: number;
  userId?: number;
  petName: string;
  avatarUrl?: string;
  petType?: string;
  breed?: string;
  gender?: number;
  birthDate?: string;
  weight?: number;
  height?: number;
  description?: string;
}

export interface PetPayload {
  petName: string;
  avatarUrl?: string;
  petType?: string;
  breed?: string;
  gender?: number;
  birthDate?: string;
  weight?: number;
  height?: number;
  description?: string;
}

export function listPets(): Promise<Pet[]> {
  return request<Pet[]>({ url: '/pet' });
}

export function listPetsPage(
  page = 1,
  size = 20,
  keyword?: string,
): Promise<PageData<Pet>> {
  return request<PageData<Pet>>({
    url: '/pet/page',
    data: { page, size, keyword },
  });
}

export function getPet(petId: number): Promise<Pet> {
  return request<Pet>({ url: `/pet/${petId}` });
}

export function createPet(payload: PetPayload): Promise<number> {
  return request<number>({
    url: '/pet',
    method: 'POST',
    data: payload,
    showLoading: true,
  });
}

export function updatePet(petId: number, payload: PetPayload): Promise<void> {
  return request<void>({
    url: `/pet/${petId}`,
    method: 'PUT',
    data: payload,
    showLoading: true,
  });
}

export function deletePet(petId: number): Promise<void> {
  return request<void>({
    url: `/pet/${petId}`,
    method: 'DELETE',
    showLoading: true,
  });
}
