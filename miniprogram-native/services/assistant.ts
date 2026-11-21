import { request } from '../utils/request';
export interface AssistantSession { id: string; title: string; updatedAt: string }
export interface AssistantProduct {
  id: string; name: string; subtitle: string; coverUrl: string; skuId: string; specText: string;
  priceCents: number; memberPriceCents?: number; availableQty: number; detailPath: string;
  coverSrc?: string; priceText?: string; memberPriceText?: string;
}
export interface AssistantTurn {
  sessionId: string; turnId: string; requestId: string; status: 'PENDING' | 'SUCCEEDED' | 'FAILED';
  petId?: number;
  question: string; content?: string; products: AssistantProduct[]; createdAt: string;
  photoUrls?: string[];
  photoSources?: string[];
  draft?: AlbumDraft;
  draftTimeText?: string;
}
export interface AlbumDraft {
  title: string; content: string; location: string; occurredAt: string;
  photoUrls: string[]; petId: number; diaryId?: string;
  mood?: string; category?: string; isPublic?: number;
}
export interface AssistantHistory { records: AssistantTurn[]; nextBeforeId?: string; hasMore: boolean }
export const listAssistantSessions = () => request<AssistantSession[]>({ url: '/assistant/sessions' });
export const createAssistantSession = () => request<AssistantSession>({ url: '/assistant/sessions', method: 'POST' });
export const getAssistantHistory = (id: string, beforeId?: string) => request<AssistantHistory>({
  url: `/assistant/sessions/${id}/messages`, data: { beforeId },
});
export const sendAssistantMessage = (id: string, requestId: string, content: string, petId?: number, photoUrls?: string[]) => request<AssistantTurn>({
  url: `/assistant/sessions/${id}/messages`, method: 'POST', data: { requestId, content, petId, photoUrls }, timeout: 90000,
});
export const saveAssistantAlbum = (sessionId: string, turnId: string, draft: AlbumDraft) => request<AlbumDraft>({
  url: `/assistant/sessions/${sessionId}/messages/${turnId}/album`, method: 'POST', data: { ...draft },
});
