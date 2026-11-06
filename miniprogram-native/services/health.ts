import { request } from '../utils/request';
import type { PageData } from '../types/page';

export interface WeightRecord {
  id: number;
  petId: number;
  weightKg: number;
  recordedAt: string;
  createdAt?: string;
}

export interface FeedingRecord {
  id: number;
  petId: number;
  feedType: string;
  amountG?: number;
  mealTime: string;
  notes?: string;
  createdAt?: string;
}

export interface VaccineRecord {
  id: number;
  petId: number;
  vaccineName: string;
  vaccinatedAt: string;
  nextDueAt?: string;
  vetClinic?: string;
  notes?: string;
  createdAt?: string;
}

export interface CheckupRecord {
  id: number;
  petId: number;
  checkupDate: string;
  vetClinic?: string;
  findings?: string;
  recommendations?: string;
  createdAt?: string;
}

export interface WeightRecordPayload {
  petId: number;
  weightKg: number;
  recordedAt?: string;
}

export interface FeedingRecordPayload {
  petId: number;
  feedType: string;
  amountG?: number;
  mealTime?: string;
  notes?: string;
}

export interface VaccineRecordPayload {
  petId: number;
  vaccineName: string;
  vaccinatedAt: string;
  nextDueAt?: string;
  vetClinic?: string;
  notes?: string;
}

export interface CheckupRecordPayload {
  petId: number;
  checkupDate: string;
  vetClinic?: string;
  findings?: string;
  recommendations?: string;
}

export function getWeightRecords(petId: number): Promise<WeightRecord[]> {
  return request<WeightRecord[]>({ url: '/health/weight', data: { petId } });
}

export function getWeightRecordPage(
  petId: number,
  page = 1,
  size = 10,
): Promise<PageData<WeightRecord>> {
  return request<PageData<WeightRecord>>({
    url: '/health/weight/page',
    data: { petId, page, size },
  });
}

export function addWeightRecord(payload: WeightRecordPayload): Promise<number> {
  return request<number>({
    url: '/health/weight',
    method: 'POST',
    data: payload,
    showLoading: true,
  });
}

export function deleteWeightRecord(recordId: number): Promise<void> {
  return request<void>({
    url: `/health/weight/${recordId}`,
    method: 'DELETE',
    showLoading: true,
  });
}

export function getFeedingRecords(
  petId: number,
  date?: string,
): Promise<FeedingRecord[]> {
  return request<FeedingRecord[]>({
    url: '/health/feeding',
    data: date ? { petId, date } : { petId },
  });
}

export function getFeedingRecordPage(
  petId: number,
  date: string | undefined,
  page = 1,
  size = 10,
): Promise<PageData<FeedingRecord>> {
  return request<PageData<FeedingRecord>>({
    url: '/health/feeding/page',
    data: date ? { petId, date, page, size } : { petId, page, size },
  });
}

export function addFeedingRecord(
  payload: FeedingRecordPayload,
): Promise<number> {
  return request<number>({
    url: '/health/feeding',
    method: 'POST',
    data: payload,
    showLoading: true,
  });
}

export function deleteFeedingRecord(recordId: number): Promise<void> {
  return request<void>({
    url: `/health/feeding/${recordId}`,
    method: 'DELETE',
    showLoading: true,
  });
}

export function getVaccineRecords(petId: number): Promise<VaccineRecord[]> {
  return request<VaccineRecord[]>({ url: '/health/vaccine', data: { petId } });
}

export function getVaccineRecordPage(
  petId: number,
  page = 1,
  size = 10,
): Promise<PageData<VaccineRecord>> {
  return request<PageData<VaccineRecord>>({
    url: '/health/vaccine/page',
    data: { petId, page, size },
  });
}

export function addVaccineRecord(
  payload: VaccineRecordPayload,
): Promise<number> {
  return request<number>({
    url: '/health/vaccine',
    method: 'POST',
    data: payload,
    showLoading: true,
  });
}

export function deleteVaccineRecord(recordId: number): Promise<void> {
  return request<void>({
    url: `/health/vaccine/${recordId}`,
    method: 'DELETE',
    showLoading: true,
  });
}

export function getCheckupRecords(petId: number): Promise<CheckupRecord[]> {
  return request<CheckupRecord[]>({ url: '/health/checkup', data: { petId } });
}

export function getCheckupRecordPage(
  petId: number,
  page = 1,
  size = 10,
): Promise<PageData<CheckupRecord>> {
  return request<PageData<CheckupRecord>>({
    url: '/health/checkup/page',
    data: { petId, page, size },
  });
}

export function addCheckupRecord(
  payload: CheckupRecordPayload,
): Promise<number> {
  return request<number>({
    url: '/health/checkup',
    method: 'POST',
    data: payload,
    showLoading: true,
  });
}

export function deleteCheckupRecord(recordId: number): Promise<void> {
  return request<void>({
    url: `/health/checkup/${recordId}`,
    method: 'DELETE',
    showLoading: true,
  });
}
