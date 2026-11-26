import { requireLogin, getToken, getUser } from '../../utils/auth';
import { resolveImage, resolveImages } from '../../utils/image';
import { uploadImage } from '../../utils/upload';
import { request } from '../../utils/request';
import { StreamingSpeech } from '../../utils/speech';
import { formatDateTime, formatMoney } from '../../utils/format';
import { listPets } from '../../services/pet';
import { assetUrl } from '../../utils/assets';
import { createAssistantSession, getAssistantHistory, saveAssistantAlbum, sendAssistantMessage } from '../../services/assistant';
import type { AlbumDraft, AssistantSession, AssistantTurn } from '../../services/assistant';
import { startAgent, agentHasTask } from '../../utils/agent-runner';

const emptyConversation = () => ({
  sessionId: '', sessions: [] as AssistantSession[], turns: [] as AssistantTurn[],
  input: '', photos: [] as { path: string; url: string }[], hasMore: false, beforeId: '',
  scrollTarget: '', errorText: '', loadError: false,
});

Page({
  data: {
    agentMode: false,
    statusBarHeight: 20,
    navHeight: 44,
    petChoices: [{ id: 0, label: '通用咨询' }],
    petChoiceIndex: 0,
    diaryPetChoices: [] as { id: number; label: string }[],
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
    photos: [] as { path: string; url: string }[],
    speechEnabled: false,
    recording: false,
    inputMode: 'voice',
    voicePhase: '',
    voiceText: '',
    cancelRecording: false,
    waveBars: Array(31).fill(6) as number[],
  },
  _speech: null as StreamingSpeech | null,
  _draftUploadId: '',
  _holding: false,
  _touchStartY: 0,
  _voiceOriginal: '',
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
    request<boolean>({ url: '/assistant/speech/status' }).then(speechEnabled => {
      if (this._alive) this.setData({ speechEnabled });
    }).catch(() => {});
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
  onHide() {
    if (this.data.recording) this.cancelVoice();
    else this.finishVoiceDraft();
  },
  onUnload() { this._alive = false; this._generation++; this._speech?.cancel(); },
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
      this.setData({ petChoices: choices, diaryPetChoices: choices.slice(1), petChoiceIndex: index }, () => this.updateGreeting());
    } catch (_) { /* 档案不可用时继续通用咨询。 */ }
  },
  choosePet(event: any) {
    if (this.data.busy || this.data.loading) return;
    const index = Number(event.detail.value);
    if (!this.data.petChoices[index] || index === this.data.petChoiceIndex) return;
    this.finishVoiceDraft();
    this._generation++;
    this.closeRecommendation();
    const { petChoices, petChoiceIndex, sessionId, sessions, turns, input, photos, hasMore,
      beforeId, scrollTarget, errorText, loadError } = this.data;
    this._petConversations[petChoices[petChoiceIndex].id] = {
      sessionId, sessions, turns, input, photos, hasMore, beforeId, scrollTarget, errorText, loadError,
    };
    this.setData({ ...(this._petConversations[petChoices[index].id] || emptyConversation()),
      petChoiceIndex: index }, () => {
      this.updateGreeting();
      if (this.data.turns.length) this.bottom();
    });
  },
  async loadArtwork() {
    const names = ['hero', 'send', 'camera-black-v2', 'keyboard-black-v2', 'voice-black-v2'];
    const entries = await Promise.all(names.map(async name => [name,
      await resolveImage(assetUrl(`miniapp-v1/assistant-v2/${name}.${name === 'hero' ? 'webp' : name === 'send' ? 'png' : 'svg'}`))]));
    if (this._alive) {
      const artwork = Object.fromEntries(entries);
      artwork.camera = artwork['camera-black-v2'];
      artwork.keyboard = artwork['keyboard-black-v2'];
      artwork.voice = artwork['voice-black-v2'];
      this.setData({ artwork });
    }
  },
  artworkError(e: any) {
    const name = e.currentTarget.dataset.name;
    if (name) this.setData({ [`artwork.${name}`]: '' });
  },
  async decorate(turns: AssistantTurn[]): Promise<AssistantTurn[]> {
    return Promise.all(turns.map(async t => ({ ...t,
      draft: t.draft ? { ...t.draft, mood: t.draft.mood || 'happy', category: t.draft.category || 'daily',
        isPublic: t.draft.isPublic || 0 } : undefined,
      photoSources: t.photoUrls?.length ? await resolveImages(t.photoUrls) : [],
      draftTimeText: t.draft ? formatDateTime(t.draft.occurredAt, true) : '',
      products: await Promise.all((t.products || []).map(async p => ({
      ...p, coverSrc: await resolveImage(p.coverUrl), priceText: formatMoney(p.priceCents),
      memberPriceText: p.memberPriceCents != null && p.memberPriceCents < p.priceCents ? formatMoney(p.memberPriceCents) : '',
    }))) })));
  },
  async newSession(keepComposer = false) {
    if (this.data.busy || this.data.loading) return;
    this.finishVoiceDraft();
    const g = ++this._generation; this.setData({ loading: true, errorText: '' });
    try {
      const session = await createAssistantSession();
      if (!this.valid(g)) return;
      this.setData({ sessions: [session, ...this.data.sessions].slice(0, 30), sessionId: session.id,
        turns: [], input: keepComposer === true ? this.data.input : '',
        photos: keepComposer === true ? this.data.photos : [], hasMore: false, beforeId: '', loading: false });
    } catch (_) { if (this.valid(g)) this.setData({ loading: false }); }
  },
  async openSession(id: string) {
    if (this.data.busy) return;
    const g = ++this._generation;
    this.setData({ loading: true, loadError: false, sessionId: id, turns: [], input: '', photos: [], errorText: '' });
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
  finishVoiceDraft() {
    if (this.data.voicePhase !== 'finishing') return;
    const speech = this._speech;
    this._speech = null;
    this.setData({ voicePhase: '' });
    speech?.cancel();
  },
  input(e: any) {
    this.finishVoiceDraft();
    this.setData({ input: e.detail.value });
  },
  toggleInputMode() {
    if (this.data.busy || this.data.loading) return;
    this.finishVoiceDraft();
    this.setData({ inputMode: this.data.inputMode === 'voice' ? 'text' : 'voice' });
  },
  moveVoice(event: { touches: { clientY: number }[] }) {
    if (!this._holding || !event.touches.length) return;
    this.setData({ cancelRecording: this._touchStartY - event.touches[0].clientY > 60 });
  },
  endVoice() {
    if (!this._holding) return;
    this._holding = false;
    if (this.data.cancelRecording || this.data.voicePhase === 'connecting') {
      this.cancelVoice();
      return;
    }
    this.setData({ recording: false, busy: false, inputMode: 'text', voicePhase: 'finishing',
      cancelRecording: false, waveBars: Array(31).fill(6) });
    this._speech?.stop();
  },
  cancelVoice() {
    if (!this.data.recording) return;
    this._holding = false;
    this.setData({ input: this._voiceOriginal, voiceText: '' });
    this._speech?.cancel();
  },
  async beginVoice(event: { touches: { clientY: number }[] }) {
    if (this.data.busy || this.data.loading) return;
    this.finishVoiceDraft();
    if (!this.data.speechEnabled) {
      wx.showToast({ title: '语音服务暂未启用', icon: 'none' });
      return;
    }
    const original = this.data.input;
    this._voiceOriginal = original;
    this._holding = true;
    this._touchStartY = event.touches[0]?.clientY || 0;
    const generation = this._generation;
    this.setData({ recording: true, busy: true, voicePhase: 'connecting', voiceText: '',
      cancelRecording: false, waveBars: Array(31).fill(6) });
    const speech = new StreamingSpeech(text => {
      if (this.valid(generation) && this._speech === speech) {
        this.setData({ voiceText: text, input: (original + text).slice(0, 2000) });
      }
    }, error => {
      if (!this.valid(generation) || this._speech !== speech) return;
      this._speech = null;
      this._holding = false;
      this.setData({ recording: false, busy: this.data.recording ? false : this.data.busy,
        voicePhase: '', cancelRecording: false,
        inputMode: this.data.input ? 'text' : 'voice', waveBars: Array(31).fill(6) });
      if (error) wx.showToast({ title: error, icon: 'none' });
    }, level => {
      if (!this.valid(generation) || !this._holding || this.data.voicePhase !== 'listening') return;
      this.setData({ waveBars: [...this.data.waveBars.slice(1), Math.round(6 + level * 64)] });
    }, () => {
      if (this.valid(generation) && this._holding) this.setData({ voicePhase: 'listening' });
      else speech.cancel();
    });
    this._speech = speech;
    try { await speech.start(); }
    catch (_) {
      speech.cancel();
      if (this.valid(generation)) wx.showToast({ title: '语音连接创建失败', icon: 'none' });
    }
  },
  async choosePhotos() {
    const { busy, loading, photos } = this.data;
    if (busy || loading || photos.length >= 3) return;
    this.finishVoiceDraft();
    const generation = this._generation;
    this.setData({ busy: true });
    try {
      const selection = await new Promise<{ tempFiles: { tempFilePath: string }[] }>((resolve, reject) => {
        wx.chooseMedia({ count: 3 - photos.length, mediaType: ['image'], success: resolve, fail: reject });
      });
      for (const file of selection.tempFiles) {
        const url = await uploadImage(file.tempFilePath, 'assistant');
        if (!this.valid(generation)) return;
        this.setData({ photos: [...this.data.photos, { path: file.tempFilePath, url }] });
      }
    } catch (_) {
      // 取消选择无需提示，上传错误由上传服务显示。
    } finally {
      if (this.valid(generation)) this.setData({ busy: false });
    }
  },
  removePhoto(e: { currentTarget: { dataset: { index: number } } }) {
    if (this.data.busy) return;
    this.setData({ photos: this.data.photos.filter((_, index) => index !== e.currentTarget.dataset.index) });
  },
  changeInlineDiary(e: { currentTarget: { dataset: { id: string } }; detail: AlbumDraft }) {
    if (this.data.loading || (this.data.busy && this._draftUploadId !== e.currentTarget.dataset.id)) return;
    const turn = this.data.turns.find(item => item.requestId === e.currentTarget.dataset.id);
    if (!turn?.draft || turn.draft.diaryId) return;
    this.replace({ ...turn, draft: { ...e.detail, diaryId: undefined } });
  },
  inlineDiaryUploading(e: { detail: boolean; currentTarget: { dataset: { id: string } } }) {
    this._draftUploadId = e.detail ? e.currentTarget.dataset.id : '';
    this.setData({ busy: e.detail });
  },
  async confirmAlbum(e: { currentTarget: { dataset: { id?: string } } }) {
    if (this.data.busy || this.data.loading) return;

    const requestId = e.currentTarget.dataset.id;
    const turn = this.data.turns.find(item => item.requestId === requestId);
    const draft = turn?.draft;
    if (!turn?.turnId || !draft || turn.draft?.diaryId) return;
    if (!draft.title.trim() || !draft.content.trim()) {
      wx.showToast({ title: '请填写标题和记录内容', icon: 'none' });
      return;
    }
    const generation = this._generation;
    this.setData({ busy: true });
    try {
      const saved = await saveAssistantAlbum(turn.sessionId, turn.turnId, draft);
      if (!this.valid(generation)) return;
      this.replace({ ...turn, draft: saved, draftTimeText: formatDateTime(saved.occurredAt, true) });

      wx.showToast({ title: '已保存到成长相册', icon: 'success' });
    } catch (_) {
      // 请求层已显示提示，保留草稿供重试。
    } finally {
      if (this.valid(generation)) this.setData({ busy: false });
    }
  },
  viewAlbumRecord(e: { currentTarget: { dataset: { id: string } } }) {
    wx.navigateTo({ url: `/pages/diary/detail?id=${e.currentTarget.dataset.id}` });
  },
  async quick(e: any) {
    if (this.data.busy || this.data.loading) return;
    const question = e.currentTarget.dataset.text;
    if (typeof question !== 'string' || !question.trim()) return;
    this.setData({ input: question });
    await this.send();
  },
  toggleAgentMode(event: any) {
    this.setData({ agentMode: !!event.detail.value });
  },
  async send() {
    const content = this.data.input.trim();
    if (!content || content.length > 2000 || this.data.busy || this.data.loading) return;
    this.finishVoiceDraft();
    if (this.data.agentMode) {
      if (agentHasTask()) {
        wx.showToast({ title: '请先结束当前操作任务', icon: 'none' });
        return;
      }
      if (this.data.photos.length) {
        wx.showToast({ title: '请先描述需求，日记照片可在任务中上传', icon: 'none' });
        return;
      }
      if (!this.data.sessionId) {
        await this.newSession(true);
        if (!this.data.sessionId) return;
      }
      this.setData({ input: '', busy: true });
      try {
        await startAgent(content, this.data.sessionId, this.data.petChoices[this.data.petChoiceIndex]?.id || undefined);
      } catch (error) {
        wx.showToast({ title: error instanceof Error ? error.message : '操作启动失败', icon: 'none' });
      } finally {
        if (this._alive) this.setData({ busy: false });
      }
      return;
    }
    if (/^(保存这条|确认保存|保存到成长相册)[。！!？?]?$/.test(content) && !this.data.photos.length) {
      const pending = this.data.turns.filter(turn => turn.draft && !turn.draft.diaryId);
      if (pending.length === 1) {
        await this.confirmAlbum({ currentTarget: { dataset: { id: pending[0].requestId } } });
        if (this.data.turns.find(turn => turn.requestId === pending[0].requestId)?.draft?.diaryId) {
          this.setData({ input: '' });
        }
        return;
      }
      if (pending.length > 1) {
        wx.showToast({ title: '请点击要保存的草稿卡片', icon: 'none' });
        return;
      }
    }
    if (!this.data.sessionId) { await this.newSession(true); if (!this.data.sessionId) return; }
    const requestId = `mp_${Date.now()}_${Math.random().toString(36).slice(2, 12)}`;
    const turn: AssistantTurn = { sessionId: this.data.sessionId, turnId: '', requestId, status: 'PENDING', petId: this.data.petChoices[this.data.petChoiceIndex]?.id || undefined, question: content, products: [], createdAt: '' };
    const photos = this.data.photos;
    turn.photoUrls = photos.map(photo => photo.url);
    turn.photoSources = photos.map(photo => photo.path);
    this.setData({ turns: [...this.data.turns, turn], input: '', photos: [] });
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
      if (retry) {
        this.setData({ busy: false });
        await this.execute(found ? { ...found, petId: turn.petId, photoUrls: turn.photoUrls,
          photoSources: turn.photoSources } : turn);
      }
      else this.setData({ errorText: '回复暂未完成，请查询状态后重试。' });
    } catch (_) { if (this.valid(g)) this.setData({ errorText: '暂时无法确认结果，请稍后查询状态。' }); }
    finally { if (this.valid(g)) this.setData({ busy: false }); }
  },
  async execute(turn: AssistantTurn) {
    if (this.data.busy) return;
    const g = this._generation; this.setData({ busy: true, errorText: '' });
    this.replace({ ...turn, status: 'PENDING' }); this.bottom();
    try {
      const reply = await sendAssistantMessage(turn.sessionId, turn.requestId, turn.question, turn.petId, turn.photoUrls);
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
