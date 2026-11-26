const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

function runner(api, options = {}) {
  const storage = options.storage || new Map();
  const exports = {};
  let token = 'test-token';
  const source = fs.readFileSync(path.join(__dirname, '../../server/target/miniprogram-contract/utils/agent-runner.js'), 'utf8');
  vm.runInNewContext(source, {
    exports, require: name => name === './auth'
      ? { getToken: () => token, getUser: () => ({ id: 7 }) } : api,
    wx: { getStorageSync: key => storage.get(key), setStorageSync: (key, value) => storage.set(key, structuredClone(value)) },
    getCurrentPages: () => [{ route: options.route || 'pages/health/index' }],
    setTimeout, console,
  });
  let progress;
  exports.subscribeAgent(value => { progress = value; });
  return { ...exports, storage, progress: () => progress, logout: () => { token = ''; } };
}
const actionTask = (name = 'get_page_state') => ({
  id: 'task-1', status: 'ACTION', text: '正在操作', action: { id: 'action-1', name, args: {} },
});

const photoTask = () => ({
  id: 'diary-task', status: 'WAITING_PHOTOS', text: '请手动上传今天的照片',
  action: { id: 'photo-action', name: 'request_photos', args: {} },
  pageState: { page: 'diary', content: '今天一起散步', photoUrls: [] },
});

test('photo requests wait for a user gesture before opening the picker and resume after upload', async () => {
  let picks = 0;
  let posted;
  const r = runner({
    createAgentTask: async () => photoTask(),
    advanceAgentTask: async (_id, result) => { posted = result; return { id: 'diary-task', status: 'COMPLETED', text: '日记已保存' }; },
  }, { route: 'pages/diary/edit' });
  r.registerAgentPage('diary', {
    state: () => ({ page: 'diary' }),
    execute: async action => {
      if (action.name === 'restore_diary') return { ok: true };
      picks++;
      r.setAgentForeground(false);
      r.setAgentForeground(true);
      return { ok: true, uploaded: true, state: { page: 'diary', photoUrls: ['uploaded-photo'] } };
    },
  });
  await r.startAgent('记录今天散步日记', '1');
  assert.equal(picks, 0);
  assert.equal(posted, undefined);
  await r.provideAgentPhotos();
  assert.equal(picks, 1);
  assert.equal(posted.result.uploaded, true);
  assert.equal(r.progress().task.status, 'COMPLETED');
});

test('cancelling photo selection retains the task and an explicit skip can continue it', async () => {
  let submissions = 0;
  const r = runner({
    createAgentTask: async () => photoTask(),
    advanceAgentTask: async (_id, result) => {
      submissions++;
      assert.equal(result.result.skipped, true);
      return { id: 'diary-task', status: 'COMPLETED', text: '日记已保存' };
    },
  }, { route: 'pages/diary/edit' });
  r.registerAgentPage('diary', {
    state: () => ({ page: 'diary' }), execute: async action => {
      if (action.name === 'restore_diary') return { ok: true };
      if (action.name === 'upload_photos') return { ok: false, error: '用户取消选图' };
      return { ok: true, skipped: true, state: { page: 'diary', photoUrls: [] } };
    },
  });
  await r.startAgent('记录散步日记', '1');
  await r.provideAgentPhotos();
  assert.equal(submissions, 0);
  assert.equal(r.progress().task.status, 'WAITING_PHOTOS');
  await r.provideAgentPhotos(true);
  assert.equal(submissions, 1);
});

test('a stopped photo upload cannot submit a late result', async () => {
  let resolveUpload;
  let submissions = 0;
  const r = runner({
    createAgentTask: async () => photoTask(),
    cancelAgentTask: async () => ({ id: 'diary-task', status: 'CANCELLED', text: '已停止' }),
    advanceAgentTask: async () => { submissions++; },
  }, { route: 'pages/diary/edit' });
  r.registerAgentPage('diary', {
    state: () => ({ page: 'diary' }), execute: async action => action.name === 'restore_diary'
      ? { ok: true } : new Promise(resolve => { resolveUpload = resolve; }),
  });
  await r.startAgent('记录散步日记', '1');
  const upload = r.provideAgentPhotos();
  await r.stopAgent();
  resolveUpload({ ok: true, state: { photoUrls: ['late-upload'] } });
  await upload;
  assert.equal(submissions, 0);
  assert.equal(r.progress().task.status, 'CANCELLED');
});

test('a restored photo request restores its diary snapshot without choosing photos', async () => {
  const task = photoTask();
  const storage = new Map([['itsworld_agent_task_7', { task }]]);
  const r = runner({ getAgentTask: async () => task }, { route: 'pages/diary/edit', storage });
  let restored;
  r.registerAgentPage('diary', {
    state: () => ({ page: 'diary' }), execute: async action => {
      assert.equal(action.name, 'restore_diary');
      restored = action.args.state;
      return { ok: true };
    },
  });
  await r.resumeAgent();
  assert.equal(restored.content, '今天一起散步');
});

test('a restarted upload result restores diary fields before executing the next action', async () => {
  const task = photoTask();
  const state = { page: 'diary', content: '已填写的日记', photoUrls: ['uploaded-photo'], photosAnswered: true };
  const storage = new Map([['itsworld_agent_task_7', {
    task, result: { requestId: 'uploaded-result', actionId: 'photo-action', result: { ok: true, state } },
  }]]);
  let restored = false;
  let advances = 0;
  const r = runner({ advanceAgentTask: async () => {
    if (++advances === 1) return { ...actionTask('get_page_state'), pageState: state };
    return { id: 'task-1', status: 'COMPLETED', text: '日记已保存' };
  } }, { route: 'pages/diary/edit', storage });
  r.registerAgentPage('diary', {
    state: () => ({ page: 'diary' }), execute: async action => {
      if (action.name === 'restore_diary') {
        assert.equal(action.args.state.photoUrls[0], 'uploaded-photo');
        restored = true;
      } else {
        assert.equal(restored, true);
      }
      return { ok: true };
    },
  });
  await r.resumeAgent();
  assert.equal(r.progress().task.status, 'COMPLETED');
});

test('diary upload retains successful photos when a later upload fails', async () => {
  let page;
  let uploads = 0;
  const source = fs.readFileSync(path.join(__dirname, '../../server/target/miniprogram-contract/pages/diary/edit.js'), 'utf8');
  vm.runInNewContext(source, {
    exports: {}, Page: options => { page = options; },
    wx: { chooseMedia: options => options.success({ tempFiles: [{ tempFilePath: 'one' }, { tempFilePath: 'two' }] }) },
    require: name => {
      if (name.endsWith('agent-runner')) return { assertAgentAction() {} };
      if (name.endsWith('upload')) return { uploadImage: async (_path, type) => {
        assert.equal(type, 'assistant');
        if (++uploads === 2) throw new Error('network failure');
        return 'first-uploaded-photo';
      } };
      if (name.endsWith('image')) return { resolveImages: async urls => urls };
      return {};
    },
  });
  const context = { ...page, data: structuredClone(page.data), setData(values) { Object.assign(this.data, values); } };
  const uploaded = await page.choosePhotos.call(context, undefined, { taskId: 'task', actionId: 'action' });
  assert.equal(uploaded, false);
  assert.equal(context.data.photos.length, 1);
  assert.equal(context.data.photos[0], 'first-uploaded-photo');
  assert.equal(context.data.uploading, false);
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
