const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

function runner(api) {
  const storage = new Map();
  const exports = {};
  let token = 'test-token';
  const source = fs.readFileSync(path.join(__dirname, '../../server/target/miniprogram-contract/utils/agent-runner.js'), 'utf8');
  vm.runInNewContext(source, {
    exports, require: name => name === './auth'
      ? { getToken: () => token, getUser: () => ({ id: 7 }) } : api,
    wx: { getStorageSync: key => storage.get(key), setStorageSync: (key, value) => storage.set(key, structuredClone(value)) },
    getCurrentPages: () => [{ route: 'pages/health/index' }],
    setTimeout, console,
  });
  let progress;
  exports.subscribeAgent(value => { progress = value; });
  return { ...exports, storage, progress: () => progress, logout: () => { token = ''; } };
}
const actionTask = (name = 'get_page_state') => ({
  id: 'task-1', status: 'ACTION', text: '正在操作', action: { id: 'action-1', name, args: {} },
});

test('a lost result response is replayed with the same request ID without executing the page twice', async () => {
  let executions = 0;
  const submissions = [];
  const r = runner({
    createAgentTask: async () => actionTask(),
    advanceAgentTask: async (_id, result) => {
      submissions.push(result);
      if (submissions.length === 1) throw new Error('response lost');
      return { id: 'task-1', status: 'COMPLETED', text: '已保存' };
    },
  });
  r.registerHealthPage({ state: () => ({}), execute: async () => { executions++; return { ok: true }; } });
  await r.startAgent('记录体重', '1');
  assert.equal(r.progress().paused, true);
  await r.resumeAgent();
  assert.equal(executions, 1);
  assert.equal(submissions[0].requestId, submissions[1].requestId);
  assert.equal(r.progress().task.status, 'COMPLETED');
});

test('backgrounding during a page action retains its result and dispatches no further action', async () => {
  let calls = 0;
  const r = runner({
    createAgentTask: async () => actionTask(),
    advanceAgentTask: async () => { calls++; return { id: 'task-1', status: 'COMPLETED', text: '完成' }; },
  });
  r.registerHealthPage({ state: () => ({}), execute: async () => {
    r.setAgentForeground(false);
    return { ok: true };
  } });
  await r.startAgent('记录体重', '1');
  assert.equal(calls, 0);
  r.setAgentForeground(true);
  await r.resumeAgent();
  assert.equal(calls, 1);
});

test('stopping an in-flight decision prevents the returned action from executing', async () => {
  let resolveCreate;
  let executions = 0;
  const r = runner({
    createAgentTask: () => new Promise(resolve => { resolveCreate = resolve; }),
    cancelAgentTask: async () => ({ id: 'task-1', status: 'CANCELLED', text: '已停止' }),
  });
  r.registerHealthPage({ state: () => ({}), execute: async () => { executions++; return {}; } });
  const started = r.startAgent('记录体重', '1');
  const firstCreate = resolveCreate;
  const stopped = r.stopAgent();
  resolveCreate(actionTask());
  await stopped;
  firstCreate(actionTask());
  await started;
  assert.equal(executions, 0);
  assert.equal(r.progress().task.status, 'CANCELLED');
});

test('an account token change invalidates a pending model response', async () => {
  let resolveCreate;
  let executions = 0;
  const r = runner({ createAgentTask: () => new Promise(resolve => { resolveCreate = resolve; }) });
  r.registerHealthPage({ state: () => ({}), execute: async () => { executions++; return {}; } });
  const started = r.startAgent('记录体重', '1');
  r.logout();
  resolveCreate(actionTask());
  await started;
  assert.equal(executions, 0);
});

test('chat page sends page-operation requests into the global runner', async () => {
  let page;
  let submitted;
  const source = fs.readFileSync(path.join(__dirname, '../../server/target/miniprogram-contract/pages/assistant/chat.js'), 'utf8');
  vm.runInNewContext(source, {
    exports: {}, Page: options => { page = options; },
    require: name => name.endsWith('agent-runner') ? {
      agentHasTask: () => false,
      startAgent: async (...args) => { submitted = args; },
    } : {},
  });
  const context = {
    ...page, data: { ...page.data, input: '记录今天体重4.2公斤', agentMode: true, sessionId: '88', petChoices: [{ id: 7 }] },
    finishVoiceDraft() {}, setData(values) { Object.assign(this.data, values); },
    execute() { throw new Error('unexpected chat message path'); },
  };
  await page.send.call(context);
  assert.deepEqual(Array.from(submitted), ['记录今天体重4.2公斤', '88', 7]);
  assert.equal(context.data.busy, false);
});

test('health page rejects stale form submissions and impossible dates', async () => {
  let page;
  const source = fs.readFileSync(path.join(__dirname, '../../server/target/miniprogram-contract/pages/health/index.js'), 'utf8');
  vm.runInNewContext(source, {
    exports: {}, Page: options => { page = options; },
    require: name => {
      if (name.endsWith('static-images')) return { withStaticImages: options => options };
      if (name.endsWith('agent-runner')) return { assertAgentAction() {} };
      return {};
    },
  });
  const context = {
    ...page, data: { ...page.data, currentPet: { id: 7, petName: '团团' }, sheetVisible: true,
      sheetType: 'weight', weightValue: '4.2', weightDate: '2026-11-23' }, _agentRevision: 3,
    saveRecord() { throw new Error('unexpected save'); },
  };
  const stale = await page.executeAgentAction.call(context, {
    id: 'action', name: 'submit_record', args: { petId: '7', weightKg: 4.2, date: '2026-11-23', revision: 2 },
  }, 'task');
  assert.equal(stale.ok, false);
  const invalid = await page.executeAgentAction.call(context, {
    id: 'action', name: 'set_form_fields', args: { weightKg: 4.2, date: '2026-02-30' },
  }, 'task');
  assert.equal(invalid.ok, false);
});
