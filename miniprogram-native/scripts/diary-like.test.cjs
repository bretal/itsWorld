const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

function album(service) {
  let page;
  const dependencies = {
    '../../utils/static-images': { withStaticImages: options => options },
    '../../services/diary': service,
    '../../utils/format': { formatDate: value => value },
    '../../utils/image': { resolveImages: async value => value },
  };
  vm.runInNewContext(fs.readFileSync(path.join(__dirname,
    '../../server/target/miniprogram-contract/pages/album/index.js'), 'utf8'), {
    exports: {}, require: name => dependencies[name] || {}, Page: value => page = value,
  });
  return { ...page, data: { ...page.data, currentPet: { id: 1 } }, setData(values) {
    for (const [key, value] of Object.entries(values)) {
      const match = key.match(/^diaries\[(\d+)\]\.(\w+)$/);
      if (match) this.data.diaries[+match[1]][match[2]] = value;
      else this.data[key] = value;
    }
  } };
}

test('album reload keeps server liked state and count; tapping liked item unlikes', async () => {
  let liked = true;
  let likes = 0;
  let unlikes = 0;
  const diary = () => ({ id: 7, petId: 1, diaryDate: '2026-11-19', liked, likeCount: liked ? 1 : 0 });
  const page = album({
    listDiariesPage: async () => ({ records: [diary()], pageNum: 1, total: 1, totalPages: 1 }),
    likeDiary: async () => { likes++; liked = true; },
    unlikeDiary: async () => { unlikes++; liked = false; },
    getDiary: async () => diary(),
  });
  await page.loadDiaries(true);
  assert.equal(page.data.diaries[0].liked, true);
  await page.like({ currentTarget: { dataset: { index: 0 } } });
  assert.equal(unlikes, 1);
  assert.equal(likes, 0);
  assert.equal(page.data.diaries[0].likeCount, 0);
  await page.loadDiaries(true);
  assert.equal(page.data.diaries[0].liked, false);
  await page.like({ currentTarget: { dataset: { index: 0 } } });
  await page.loadDiaries(true);
  assert.equal(page.data.diaries[0].liked, true);
  assert.equal(page.data.diaries[0].likeCount, 1);
});

test('album blocks repeated taps while pending and uses authoritative count', async () => {
  let complete;
  let likes = 0;
  const page = album({
    likeDiary: () => { likes++; return new Promise(resolve => complete = resolve); },
    getDiary: async () => ({ id: 7, liked: true, likeCount: 5 }),
  });
  page.data.diaries = [{ id: 7, liked: false, likeCount: 5 }];
  const event = { currentTarget: { dataset: { index: 0 } } };
  const pending = page.like(event);
  await page.like(event);
  assert.equal(likes, 1);
  // 点赞中触发加载下一页，不应让这一条永久停在请求中状态。
  page.diaryRequest++;
  complete();
  await pending;
  assert.equal(page.data.diaries[0].likeCount, 5);
  assert.equal(page.data.diaries[0].liking, false);
});
