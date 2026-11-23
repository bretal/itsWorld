import { request } from '../utils/request';
import type { WeightRecordPayload } from './health';

export interface PageAction {
  id: string;
  name: string;
  args: Record<string, any>;
}
export interface AgentTask {
  id: string;
  sessionId: string;
  question: string;
  status: 'ACTION' | 'ASK_USER' | 'COMPLETED' | 'CANCELLED' | 'FAILED';
  text: string;
  action?: PageAction;
  recordId?: string;
}
export interface CreateTask {
  requestId: string;
  sessionId: string;
  content: string;
  petId?: number;
}
export interface TaskResult {
  requestId: string;
  actionId?: string;
  result?: Record<string, any>;
  reply?: string;
}
export const createAgentTask = (data: CreateTask) => request<AgentTask>({
  url: '/assistant/tasks', method: 'POST', data, timeout: 35000,
});
export const getAgentTask = (id: string) => request<AgentTask>({ url: `/assistant/tasks/${id}` });
export const advanceAgentTask = (id: string, data: TaskResult) => request<AgentTask>({
  url: `/assistant/tasks/${id}/results`, method: 'POST', data, timeout: 35000,
});
export const cancelAgentTask = (id: string) => request<AgentTask>({
  url: `/assistant/tasks/${id}/cancel`, method: 'POST',
});
export const saveAgentWeight = (taskId: string, actionId: string, data: WeightRecordPayload) => request<string>({
  url: `/assistant/tasks/${taskId}/actions/${actionId}/weight`, method: 'POST', data,
});
