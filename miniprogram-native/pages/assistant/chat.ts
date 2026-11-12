import { requireLogin, getToken, getUser } from '../../utils/auth';
import { resolveImage } from '../../utils/image';
import { formatMoney } from '../../utils/format';
import { listPets } from '../../services/pet';
import { assetUrl } from '../../utils/assets';
import { createAssistantSession, getAssistantHistory, sendAssistantMessage } from '../../services/assistant';
import type { AssistantSession, AssistantTurn } from '../../services/assistant';

const emptyConversation = () => ({
  sessionId: '', sessions: [] as AssistantSession[], turns: [] as AssistantTurn[],
  input: '', hasMore: false, beforeId: '', scrollTarget: '', errorText: '', loadError: false,
});

Page({
  data: {
    statusBarHeight: 20,
    navHeight: 44,
    petChoices: [{ id: 0, label: '通用咨询' }],
    petChoiceIndex: 0,
    sessions: [] as AssistantSession[], turns: [] as AssistantTurn[], sessionId: '', input: '',
    busy: false, loading: false, loadError: false, hasMore: false, beforeId: '', scrollTarget: '',
    artwork: {} as Record<string, string>,
    quickQuestions: [
      '今天要留意哪些健康信号？',
      '给毛孩子洗澡要注意什么？',
      '推荐几款 100 元以内的零食',
    ],
    greeting: '',
    errorText: '',
    detailId: '',
  },
  _alive: true,
  _generation: 0,
  _token: '',
  _petConversations: {} as Record<number, ReturnType<typeof emptyConversation>>,
  onLoad() {
    if (!requireLogin()) return;
    const windowInfo = wx.getWindowInfo ? wx.getWindowInfo() : wx.getSystemInfoSync();
    const statusBarHeight = windowInfo.statusBarHeight || 20;
    const capsule = wx.getMenuButtonBoundingClientRect?.();
    const navHeight = capsule?.height > 0 && capsule.top >= statusBarHeight
      ? capsule.height + 2 * (capsule.top - statusBarHeight) : 44;
    this.setData({ statusBarHeight, navHeight });
    this._token = getToken(); this.updateGreeting(); this.loadArtwork(); this.loadPetChoices();
  },
  goBack() {
    if (getCurrentPages().length > 1) wx.navigateBack();
    else wx.switchTab({ url: '/pages/home/index' });
  },
  onShow() {
    if (this._token && this._token !== getToken()) {
      this.closeRecommendation();
      this._generation++; this._token = getToken();
      this._petConversations = {};
      this.setData({ ...emptyConversation(), busy: false, loading: false,
        petChoices: [{ id: 0, label: '通用咨询' }], petChoiceIndex: 0 });
      if (requireLogin()) { this.updateGreeting(); this.loadPetChoices(); }
    }
  },
  onUnload() { this._alive = false; this._generation++; },
  valid(g: number) { return this._alive && g === this._generation && this._token === getToken(); },
  updateGreeting() {
    const user = getUser();
    const pet = this.data.petChoices[this.data.petChoiceIndex];
    const name = pet?.id ? pet.label.split(' · ')[0] : '';
    const userName = user?.nickName?.trim() || user?.username?.trim();
    const hour = new Date().getHours();
    const timeGreeting = hour < 11 ? '早上好' : hour < 13 ? '中午好' : hour < 18 ? '下午好' : '晚上好';
    this.setData({ greeting: name
      ? timeGreeting + '，' + name + '的主人，今天有什么需要我帮助的？'
      : timeGreeting + (userName ? '，' + userName : '') + '，今天有什么需要我帮助的？' });
  },
  async loadPetChoices() {
    const token = this._token;
    try {
      const pets = await listPets();
      if (!this._alive || token !== getToken()) return;
      const currentId = Number(wx.getStorageSync('currentPetId') || 0);
      const choices = [{ id: 0, label: '通用咨询' }, ...pets.map(pet => ({
        id: pet.id,
        label: pet.petName + (pet.petType === 'cat' ? ' · 猫' : pet.petType === 'dog' ? ' · 狗' : ''),
      }))];
      const index = this.data.sessionId || this.data.busy || this.data.loading ? 0 : Math.max(0, choices.findIndex(pet => pet.id === currentId));
      this.setData({ petChoices: choices, petChoiceIndex: index }, () => this.updateGreeting());
    } catch (_) { /* 档案不可用时继续通用咨询。 */ }
  },
  choosePet(event: any) {
    if (this.data.busy || this.data.loading) return;
    const index = Number(event.detail.value);
    if (!this.data.petChoices[index] || index === this.data.petChoiceIndex) return;
    this._generation++;
    this.closeRecommendation();
    const { petChoices, petChoiceIndex, sessionId, sessions, turns, input, hasMore,
      beforeId, scrollTarget, errorText, loadError } = this.data;
    this._petConversations[petChoices[petChoiceIndex].id] = {
      sessionId, sessions, turns, input, hasMore, beforeId, scrollTarget, errorText, loadError,
    };
    this.setData({ ...(this._petConversations[petChoices[index].id] || emptyConversation()),
      petChoiceIndex: index }, () => {
      this.updateGreeting();
      if (this.data.turns.length) this.bottom();
    });
  },
  async loadArtwork() {
    const names = ['hero'];
    const entries = await Promise.all(names.map(async name => [name,
      await resolveImage(assetUrl(`miniapp-v1/assistant-v2/${name}.${name === 'hero' ? 'webp' : 'png'}`))]));
    if (this._alive) this.setData({ artwork: Object.fromEntries(entries) });
  },
  artworkError(e: any) {
    const name = e.currentTarget.dataset.name;
    if (name) this.setData({ [`artwork.${name}`]: '' });
  },
  async decorate(turns: AssistantTurn[]): Promise<AssistantTurn[]> {
    return Promise.all(turns.map(async t => ({ ...t, products: await Promise.all((t.products || []).map(async p => ({
      ...p, coverSrc: await resolveImage(p.coverUrl), priceText: formatMoney(p.priceCents),
      memberPriceText: p.memberPriceCents != null && p.memberPriceCents < p.priceCents ? formatMoney(p.memberPriceCents) : '',
    }))) })));
  },
  async newSession() {
    if (this.data.busy || this.data.loading) return;
    const g = ++this._generation; this.setData({ loading: true, errorText: '' });
    try {
      const session = await createAssistantSession();
      if (!this.valid(g)) return;
      this.setData({ sessions: [session, ...this.data.sessions].slice(0, 30), sessionId: session.id,
        turns: [], input: '', hasMore: false, beforeId: '', loading: false });
    } catch (_) { if (this.valid(g)) this.setData({ loading: false }); }
  },
  async openSession(id: string) {
    if (this.data.busy) return;
    const g = ++this._generation;
    this.setData({ loading: true, loadError: false, sessionId: id, turns: [], input: '', errorText: '' });
    try {
      const page = await getAssistantHistory(id); const turns = await this.decorate(page.records);
      if (!this.valid(g)) return;
      this.setData({ turns, hasMore: page.hasMore, beforeId: page.nextBeforeId || '', loading: false });
      this.bottom();
    } catch (_) { if (this.valid(g)) this.setData({ loading: false, loadError: true }); }
  },
  chooseSession(e: any) {
    if (this.data.busy || this.data.loading) return;
    this.openSession(e.currentTarget.dataset.id);
  },
  async older() {
    if (this.data.loading || this.data.busy || !this.data.hasMore) return;
    const g = this._generation; const anchor = this.data.turns[0]?.requestId;
    this.setData({ loading: true });
    try {
      const page = await getAssistantHistory(this.data.sessionId, this.data.beforeId);
      const turns = await this.decorate(page.records);
      if (!this.valid(g)) return;
      this.setData({ turns: [...turns, ...this.data.turns], hasMore: page.hasMore,
        beforeId: page.nextBeforeId || '', loading: false, scrollTarget: anchor ? `turn-${anchor}` : '' });
    } catch (_) { if (this.valid(g)) this.setData({ loading: false }); }
  },
  input(e: any) { this.setData({ input: e.detail.value }); },
  async quick(e: any) {
    if (this.data.busy || this.data.loading) return;
    const question = e.currentTarget.dataset.text;
    if (typeof question !== 'string' || !question.trim()) return;
    this.setData({ input: question });
    await this.send();
  },
  async send() {
    const content = this.data.input.trim();
    if (!content || content.length > 2000 || this.data.busy || this.data.loading) return;
    if (!this.data.sessionId) { await this.newSession(); if (!this.data.sessionId) return; }
    const requestId = `mp_${Date.now()}_${Math.random().toString(36).slice(2, 12)}`;
    const turn: AssistantTurn = { sessionId: this.data.sessionId, turnId: '', requestId, status: 'PENDING', petId: this.data.petChoices[this.data.petChoiceIndex]?.id || undefined, question: content, products: [], createdAt: '' };
    this.setData({ turns: [...this.data.turns, turn], input: '' });
    await this.execute(turn);
  },
  async retry(e: any) {
    if (this.data.busy || this.data.loading) return;
    const turn = this.data.turns.find(t => t.requestId === e.currentTarget.dataset.id);
    if (turn) await this.reconcile(turn, true);
  },
  async reconcile(turn: AssistantTurn, retry: boolean) {
    const g = this._generation; this.setData({ busy: true, errorText: '' });
    try {
      // Search by requestId across pages: the original request may no longer be on the newest page.
      let before: string | undefined; let found: AssistantTurn | undefined;
      do {
        const page = await getAssistantHistory(turn.sessionId, before);
        if (!this.valid(g)) return;
        found = page.records.find(t => t.requestId === turn.requestId);
        before = page.hasMore ? page.nextBeforeId : undefined;
      } while (!found && before);
      if (found) {
        const decorated = (await this.decorate([found]))[0];
        if (!this.valid(g)) return;
        this.replace({ ...decorated, petId: turn.petId });
        if (found.status === 'SUCCEEDED') return;
        if (found.status === 'PENDING') {
          this.setData({ errorText: '请求仍在处理中，请稍后点击查询状态。' }); return;
        }
      }
      if (retry) { this.setData({ busy: false }); await this.execute(found ? { ...found, petId: turn.petId } : turn); }
      else this.setData({ errorText: '回复暂未完成，请查询状态后重试。' });
    } catch (_) { if (this.valid(g)) this.setData({ errorText: '暂时无法确认结果，请稍后查询状态。' }); }
    finally { if (this.valid(g)) this.setData({ busy: false }); }
  },
  async execute(turn: AssistantTurn) {
    if (this.data.busy) return;
    const g = this._generation; this.setData({ busy: true, errorText: '' });
    this.replace({ ...turn, status: 'PENDING' }); this.bottom();
    try {
      const reply = await sendAssistantMessage(turn.sessionId, turn.requestId, turn.question, turn.petId);
      const decorated = (await this.decorate([reply]))[0];
      if (!this.valid(g)) return;
      this.replace({ ...decorated, petId: turn.petId }); this.bottom();
    } catch (_) {
      if (this.valid(g)) { this.setData({ busy: false }); await this.reconcile(turn, false); }
    } finally { if (this.valid(g)) this.setData({ busy: false }); }
  },
  replace(turn: AssistantTurn) {
    this.setData({ turns: this.data.turns.map(t => t.requestId === turn.requestId ? turn : t) });
  },
  bottom() { this.setData({ scrollTarget: '' }); wx.nextTick(() => { if (this._alive) this.setData({ scrollTarget: 'bottom' }); }); },
  retryLoad() { if (this.data.sessionId) this.openSession(this.data.sessionId); else this.newSession(); },
  product(e: any) {
    const id = String(e.currentTarget.dataset.id);
    if (/^[1-9][0-9]*$/.test(id)) this.setData({ detailId: id });
  },
  closeRecommendation() {
    this.setData({ detailId: '' });
  },
  imageError(e: any) {
    const id = String(e.currentTarget.dataset.id);
    this.setData({ turns: this.data.turns.map(t => ({ ...t, products: t.products.map(p => p.id === id ? { ...p, coverSrc: '' } : p) })) });
  },
});
