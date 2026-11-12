const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
function load(file, dependencies, globals = {}) {
  const exports = {};
  const source = fs.readFileSync(path.join(__dirname, '../../server/target/miniprogram-contract', file.replace(/\.ts$/, '.js')), 'utf8');
  vm.runInNewContext(source, { exports, require: name => dependencies[name], ...globals });
  return exports;
}
test('assistant uses service paths and only message sends override timeout', async () => {
  const calls = [];
  const api = load('services/assistant.ts', { '../utils/request': { request: options => { calls.push(options); return Promise.resolve({}); } } });
  await api.listAssistantSessions(); await api.createAssistantSession(); await api.getAssistantHistory('123', '90');
  await api.sendAssistantMessage('123', 'request_123', 'hello');
  assert.ok(calls.every(c => c.url.startsWith('/assistant/') && !c.url.includes('/api')));
  assert.equal(calls[3].timeout, 90000);
  assert.ok(calls.slice(0, 3).every(c => c.timeout === undefined));
  assert.equal(calls[3].data.requestId, 'request_123');
});
test('request keeps 15 second default and accepts assistant timeout', async () => {
  const calls = [];
  const wx = { request: options => { calls.push(options); options.success({ statusCode: 200, data: { code: 0, data: {} } }); } };
  const api = load('utils/request.ts', {
    '../config/env': { ENV: { baseUrl: 'https://example.test/api' } },
    './auth': { getToken: () => 'test', clearAuth: () => {} },
  }, { wx });
  await api.request({ url: '/commerce/products' });
  await api.request({ url: '/assistant/sessions/1/messages', timeout: 90000 });
  assert.equal(calls[0].timeout, 15000); assert.equal(calls[1].timeout, 90000);
  assert.equal(calls[1].url, 'https://example.test/api/assistant/sessions/1/messages');
});

test('every welcome question immediately sends its text and creates a session when needed', async () => {
  let page;
  const sends = [];
  let creates = 0;
  load('pages/assistant/chat.ts', {
    '../../utils/auth': { requireLogin: () => true, getToken: () => 'test' },
    '../../utils/image': { resolveImage: async url => url },
    '../../utils/format': { formatMoney: value => String(value) },
    '../../utils/assets': { assetUrl: value => value },
    '../../services/pet': { listPets: async () => [] },
    '../../services/assistant': {
      createAssistantSession: async () => { creates++; return { id: '123', title: '新对话' }; },
      listAssistantSessions: async () => [],
      sendAssistantMessage: async (id, requestId, content) => {
        sends.push({ id, requestId, content });
        return { sessionId: id, requestId, question: content, content: '回答', status: 'SUCCEEDED', products: [] };
      },
    },
  }, { Page: options => { page = options; }, wx: { nextTick: callback => callback() } });
  const questions = page.data.quickQuestions;
  for (const question of questions) {
    const context = { ...page, data: { ...page.data, turns: [], sessions: [] }, _token: 'test' };
    context.setData = changes => Object.assign(context.data, changes);
    await context.quick({ currentTarget: { dataset: { text: question } } });
    assert.equal(sends.at(-1).content, question);
    assert.equal(sends.at(-1).id, '123');
    assert.equal(context.data.turns[0].status, 'SUCCEEDED');
    assert.equal(context.data.input, '');
    context.data.busy = true;
    const count = sends.length;
    await context.quick({ currentTarget: { dataset: { text: question } } });
    assert.equal(sends.length, count);
  }
  assert.equal(sends.length, 3);
  assert.equal(creates, 3);
});
