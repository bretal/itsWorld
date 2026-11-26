import { getToken, getUser } from './auth';
import {
  createAgentTask, advanceAgentTask, getAgentTask, cancelAgentTask,
} from '../services/agent';
import type { AgentTask, CreateTask, PageAction, TaskResult } from '../services/agent';

export interface PageAdapter {
  state(): Record<string, any>;
  execute(action: PageAction, taskId: string): Promise<Record<string, any>>;
}
export type HealthAdapter = PageAdapter;
export interface AgentProgress {
  task?: AgentTask;
  text: string;
  paused: boolean;
  running: boolean;
}
interface StoredTask {
  task?: AgentTask;
  create?: CreateTask;
  result?: TaskResult;
}

let saved: StoredTask = {};
let owner = 0;
let paused = true;
let running = false;
let foreground = true;
let epoch = 0;
let text = '';
let executionToken = '';
const pages = new Map<string, PageAdapter>();
const pageRoutes: Record<string, string> = { health: 'pages/health/index', diary: 'pages/diary/edit' };
let navigating = false;
let choosingPhotos = false;
const listeners = new Set<(state: AgentProgress) => void>();
const terminal = (task?: AgentTask) => !!task && ['COMPLETED', 'CANCELLED', 'FAILED'].includes(task.status);
const requestId = () => `agent_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`;
const storageKey = () => `itsworld_agent_task_${owner}`;

function persist() {
  if (owner) wx.setStorageSync(storageKey(), saved);
  publish();
}
function publish() {
  const progress = { task: saved.task, text, paused, running };
  listeners.forEach(listener => listener(progress));
}
function loadOwner() {
  const userId = getUser()?.id || 0;
  if (owner === userId) return;
  epoch++;
  owner = userId;
  saved = owner ? wx.getStorageSync(storageKey()) || {} : {};
  paused = true;
  text = saved.task ? (terminal(saved.task) ? saved.task.text : '任务已暂停，可点击继续') : '';
}
export function subscribeAgent(listener: (state: AgentProgress) => void) {
  loadOwner();
  listeners.add(listener);
  publish();
  return () => listeners.delete(listener);
}
export function agentHasTask() {
  loadOwner();
  return !!saved.create || (!!saved.task && !terminal(saved.task));
}
export function registerHealthPage(adapter: HealthAdapter) {
  registerAgentPage('health', adapter);
}
export function unregisterHealthPage(adapter: HealthAdapter) {
  unregisterAgentPage('health', adapter);
}
export function registerAgentPage(name: string, adapter: PageAdapter) {
  pages.set(name, adapter);
}
export function unregisterAgentPage(name: string, adapter: PageAdapter) {
  if (pages.get(name) === adapter) {
    pages.delete(name);
    if (!navigating && !choosingPhotos) pauseAgent('已离开操作页面，任务暂停');
  }
}
export function setAgentForeground(visible: boolean) {
  foreground = visible;
  if (!visible && !choosingPhotos) pauseAgent('小程序已进入后台，操作暂停');
}
export function pauseAgent(reason = '操作已暂停') {
  paused = true;
  if (agentHasTask()) text = reason;
  publish();
}
export async function stopAgent() {
  loadOwner();
  paused = true;
  const currentEpoch = ++epoch;
  text = '正在停止操作';
  publish();
  try {
    if (saved.task) {
      const result = await cancelAgentTask(saved.task.id);
      if (currentEpoch !== epoch) return;
      saved = { task: result };
      text = result.text;
    } else if (saved.create) {
      // 创建响应丢失时，使用相同标识找回任务后取消。
      const task = await createAgentTask(saved.create);
      const result = await cancelAgentTask(task.id);
      if (currentEpoch !== epoch) return;
      saved = { task: result };
      text = result.text;
    }
    persist();
  } catch (_) {
    text = '停止请求未确认，操作已在本机暂停，请重试停止';
    publish();
  }
}
export function takeOverAgent() {
  if (agentHasTask()) void stopAgent();
}
export function assertAgentAction(taskId: string, actionId: string) {
  if (paused || !foreground || !running || executionToken !== getToken()
      || owner !== getUser()?.id || saved.task?.id !== taskId || saved.task.action?.id !== actionId) {
    throw new Error('操作已暂停或页面指令已过期');
  }
}
export async function startAgent(content: string, sessionId: string, petId?: number) {
  loadOwner();
  if (agentHasTask() || running) throw new Error('请先结束当前操作任务');
  saved = { create: { requestId: requestId(), content, sessionId, petId } };
  text = '正在理解操作需求';
  persist();
  await resumeAgent();
}
export async function replyAgent(reply: string) {
  if (saved.task?.status !== 'ASK_USER' || !reply.trim() || running) return;
  if (saved.result) {
    await resumeAgent();
    return;
  }
  saved.result = { requestId: requestId(), reply: reply.trim() };
  persist();
  await resumeAgent();
}

async function openAgentPage(name: string, valid: () => boolean) {
  const route = pageRoutes[name];
  if (!route) throw new Error('不支持的目标页面');
  navigating = true;
  try {
    if (getCurrentPages().slice(-1)[0]?.route !== route) {
      await new Promise<void>((resolve, reject) => wx.navigateTo({
        url: `/${route}`, success: () => resolve(), fail: reject,
      }));
    }
    const deadline = Date.now() + 12000;
    while (!pages.get(name) || pages.get(name)!.state().loading) {
      if (!valid()) throw new Error('操作已暂停');
      if (Date.now() > deadline) throw new Error('页面加载超时，请检查网络后继续');
      await new Promise(resolve => setTimeout(resolve, 50));
    }
  } finally {
    navigating = false;
  }
}
function currentAdapter() {
  const route = getCurrentPages().slice(-1)[0]?.route;
  const name = Object.keys(pageRoutes).find(key => pageRoutes[key] === route);
  return name ? pages.get(name) : undefined;
}
export function agentWaitingForPhotos() {
  loadOwner();
  return saved.task?.status === 'WAITING_PHOTOS' && !saved.result;
}
export async function provideAgentPhotos(skip = false) {
  loadOwner();
  if (!agentWaitingForPhotos() || running || !foreground || !getToken()) return;
  const adapter = currentAdapter();
  if (!adapter || adapter !== pages.get('diary')) {
    paused = true;
    text = '请返回日记编辑页后上传照片';
    publish();
    return;
  }
  const currentEpoch = epoch;
  const token = getToken();
  paused = false;
  running = true;
  executionToken = token;
  choosingPhotos = true;
  publish();
  try {
    const action = saved.task!.action!;
    const result = await adapter.execute({ ...action, name: skip ? 'skip_photos' : 'upload_photos' }, saved.task!.id);
    if (currentEpoch !== epoch || token !== getToken()) return;
    if (result.ok) {
      saved.result = { requestId: requestId(), actionId: action.id, result };
      persist();
    } else {
      text = result.error || '请选择照片，或点击暂不上传';
    }
  } catch (error) {
    if (currentEpoch === epoch) text = error instanceof Error ? error.message : '上传失败，请重试';
  } finally {
    choosingPhotos = false;
    running = false;
    if (currentEpoch === epoch) persist();
    else publish();
  }
  if (currentEpoch === epoch && token === getToken() && saved.result && foreground) await resumeAgent();
}

export async function resumeAgent() {
  loadOwner();
  if (running || !foreground || !owner || !getToken() || terminal(saved.task)) return;
  paused = false;
  running = true;
  const currentEpoch = epoch;
  const token = getToken();
  executionToken = token;
  const valid = () => currentEpoch === epoch && token === getToken() && foreground && !paused;
  publish();
  try {
    if (saved.create) {
      const task = await createAgentTask(saved.create);
      if (currentEpoch !== epoch || token !== getToken()) return;
      saved = { task };
      text = task.text;
      persist();
    } else if (saved.task && !saved.result) {
      const task = await getAgentTask(saved.task.id);
      if (currentEpoch !== epoch || token !== getToken()) return;
      saved.task = task;
      persist();
    }
    while (valid() && saved.task && !terminal(saved.task)) {
      if (saved.result) {
        const task = await advanceAgentTask(saved.task.id, saved.result);
        if (currentEpoch !== epoch || token !== getToken()) return;
        saved = { task };
        text = task.text;
        persist();
        continue;
      }
      if (saved.task.status === 'ASK_USER') break;
      if (saved.task.status === 'WAITING_PHOTOS') {
        await openAgentPage('diary', valid);
        if (!valid()) break;
        if (saved.task.action) {
          const restored = await pages.get('diary')!.execute({ ...saved.task.action, name: 'restore_diary', args: { state: saved.task.pageState } }, saved.task.id);
          if (!restored.ok) throw new Error(restored.error || '日记恢复失败');
        }
        break;
      }
      const action = saved.task.action;
      if (!action) throw new Error('任务缺少页面动作');
      text = saved.task.text;
      publish();
      let result: Record<string, any>;
      if (action.name === 'navigate') {
        await openAgentPage(action.args.page, valid);
        if (!valid()) break;
        result = { ok: true, state: pages.get(action.args.page)!.state() };
      } else {
        if (saved.task.pageState?.page === 'diary') {
          await openAgentPage('diary', valid);
          if (!valid()) break;
          const restored = await pages.get('diary')!.execute({
            ...action, name: 'restore_diary', args: { state: saved.task.pageState },
          }, saved.task.id);
          if (!restored.ok) throw new Error(restored.error || '日记恢复失败');
        }
        const adapter = currentAdapter();
        if (!adapter) throw new Error('请回到任务对应页面后继续');
        result = await adapter.execute(action, saved.task.id);
      }
      // 即使执行过程中暂停，也保留动作结果，恢复时先上报，避免重复执行。
      if (currentEpoch !== epoch || token !== getToken()) return;
      saved.result = { requestId: requestId(), actionId: action.id, result };
      persist();
    }
    if (terminal(saved.task)) text = saved.task!.text;
  } catch (error) {
    if (currentEpoch === epoch && token === getToken()) {
      paused = true;
      text = error instanceof Error ? error.message : '执行失败，请检查网络后继续';
    }
  } finally {
    running = false;
    if (currentEpoch === epoch) persist();
    else publish();
  }
}
