import { requireLogin } from '../../utils/auth';
import { listPets } from '../../services/pet';
import type { Pet } from '../../services/pet';
import { createDiary } from '../../services/diary';
import { uploadImage } from '../../utils/upload';
import { todayString } from '../../utils/format';
import { resolveImages } from '../../utils/image';
import { registerAgentPage, unregisterAgentPage, takeOverAgent, assertAgentAction, agentWaitingForPhotos, provideAgentPhotos } from '../../utils/agent-runner';
import type { PageAdapter } from '../../utils/agent-runner';
import { saveAgentDiary } from '../../services/agent';
import type { PageAction } from '../../services/agent';

Page({
  _agentAdapter: null as PageAdapter | null,
  _agentRevision: 0,
  _agentTaskId: '',
  _visible: false,
  data: {
    loading: true,
    photosAnswered: false,
    savedDiaryId: '',
    pets: [] as Pet[],
    petNames: [] as string[],
    currentPetIndex: 0,
    currentPet: null as Pet | null,
    noPets: false,
    diaryDate: '',
    today: '',
    moods: [
      { value: 'happy', label: '开心', emoji: '😊' },
      { value: 'calm', label: '平静', emoji: '😌' },
      { value: 'tired', label: '有点累', emoji: '😴' },
      { value: 'sad', label: '不开心', emoji: '😢' },
    ],
    moodIndex: 0,
    categories: [
      { value: 'daily', label: '日常' },
      { value: 'food', label: '饮食' },
      { value: 'health', label: '健康' },
    ],
    categoryIndex: 0,
    content: '',
    photos: [] as string[],
    photoPreviews: [] as string[],
    isPublic: true,
    saving: false,
    uploading: false,
  },

  onLoad() {
    if (!requireLogin()) {
      return;
    }
    const today = todayString();
    this.setData({ today, diaryDate: today });
  },

  async onShow() {
    this._visible = true;
    // 首次进入，或从「添加宠物」返回后重新加载
    if (!this.data.pets.length) {
      await this.loadPets();
    }
    if (!this._visible) return;
    this._agentAdapter = { state: () => this.agentPageState(), execute: (action, taskId) => this.executeAgentAction(action, taskId) };
    registerAgentPage('diary', this._agentAdapter);
  },
  onHide() {
    this._visible = false;
    if (this._agentAdapter) unregisterAgentPage('diary', this._agentAdapter);
  },
  onUnload() {
    this._visible = false;
    if (this._agentAdapter) unregisterAgentPage('diary', this._agentAdapter);
  },
  agentPageState() {
    return {
      page: 'diary', loading: this.data.loading, revision: this._agentRevision,
      petId: this.data.currentPet ? String(this.data.currentPet.id) : '', petName: this.data.currentPet?.petName || '',
      date: this.data.diaryDate, content: this.data.content.trim(),
      mood: this.data.moods[this.data.moodIndex]?.value,
      category: this.data.categories[this.data.categoryIndex]?.value,
      isPublic: this.data.isPublic ? 1 : 0, photoUrls: this.data.photos.slice(),
      photosAnswered: this.data.photosAnswered,
      availableActions: ['select_pet', 'set_diary_fields', 'request_photos', 'submit_diary'],
    };
  },
  async executeAgentAction(action: PageAction, taskId: string) {
    assertAgentAction(taskId, action.id);
    const fresh = this._agentTaskId !== taskId;
    this._agentTaskId = taskId;
    const update = (values: Record<string, any>) => new Promise<void>(resolve => {
      this._agentRevision++;
      this.setData(values, resolve);
    });
    const args = action.args;
    switch (action.name) {
      case 'get_page_state': break;
      case 'restore_diary': {
        const state = args.state;
        if (fresh && state?.page === 'diary') {
          const index = this.data.pets.findIndex(pet => String(pet.id) === String(state.petId));
          if (index < 0) return { ok: false, error: '日记宠物已不存在' };
          const previews = await resolveImages(state.photoUrls || []);
          assertAgentAction(taskId, action.id);
          await update({
            currentPetIndex: index, currentPet: this.data.pets[index], diaryDate: state.date,
            content: state.content, moodIndex: this.data.moods.findIndex(item => item.value === state.mood),
            categoryIndex: this.data.categories.findIndex(item => item.value === state.category),
            isPublic: state.isPublic === 1, photos: state.photoUrls || [], photoPreviews: previews,
            photosAnswered: !!state.photosAnswered,
          });
        }
        break;
      }
      case 'select_pet': {
        const index = this.data.pets.findIndex(pet => String(pet.id) === String(args.petId));
        if (index < 0) return { ok: false, error: '找不到该宠物，请向用户确认' };
        await update({ currentPetIndex: index, currentPet: this.data.pets[index] });
        break;
      }
      case 'set_diary_fields': {
        const date = String(args.date || '');
        const moodIndex = this.data.moods.findIndex(item => item.value === args.mood);
        const categoryIndex = this.data.categories.findIndex(item => item.value === args.category);
        if (!/^\d{4}-\d{2}-\d{2}$/.test(date) || !Number.isFinite(Date.parse(date))
            || new Date(date).toISOString().slice(0, 10) !== date || date > todayString()
            || typeof args.content !== 'string' || args.content.length > 1000
            || moodIndex < 0 || categoryIndex < 0 || ![0, 1].includes(args.isPublic)) {
          return { ok: false, error: '日记日期、正文、心情或分类无效，请重新填写' };
        }
        await update({ diaryDate: date, content: args.content, moodIndex, categoryIndex, isPublic: args.isPublic === 1 });
        break;
      }
      case 'upload_photos': {
        const uploaded = await this.choosePhotos(undefined, { taskId, actionId: action.id });
        if (!uploaded) return { ok: false, error: '照片尚未上传完成，可以重试或选择暂不上传' };
        await update({ photosAnswered: true });
        return { ok: true, uploaded: true, state: this.agentPageState() };
      }
      case 'skip_photos':
        await update({ photosAnswered: true });
        return { ok: true, skipped: true, state: this.agentPageState() };
      case 'submit_diary': {
        const state = this.agentPageState();
        if (!state.photosAnswered) return { ok: false, error: '请先调用request_photos等待用户上传或跳过' };
        if (args.revision !== state.revision || String(args.petId) !== state.petId
            || args.date !== state.date || args.content !== state.content || args.mood !== state.mood
            || args.category !== state.category || args.isPublic !== state.isPublic
            || JSON.stringify(args.photoUrls) !== JSON.stringify(state.photoUrls)) {
          return { ok: false, error: '日记表单已变化，请重新读取并核对', state };
        }
        const diaryId = await this.onSave(undefined, { taskId, actionId: action.id });
        return { ok: true, diaryId, state: this.agentPageState() };
      }
      default: return { ok: false, error: '当前日记页不支持这个动作' };
    }
    return { ok: true, state: this.agentPageState() };
  },
  manualEdit() {
    if (this.data.saving || this.data.savedDiaryId) return false;
    takeOverAgent();
    this._agentRevision++;
    return true;
  },

  async loadPets() {
    try {
      const pets = await listPets();
      if (!pets || !pets.length) {
        this.setData({ noPets: true });
        return;
      }

      const storedPetId = Number(wx.getStorageSync('currentPetId') || 0);
      let currentPetIndex = pets.findIndex((pet) => pet.id === storedPetId);
      if (currentPetIndex < 0) {
        currentPetIndex = 0;
      }

      this.setData({
        pets,
        petNames: pets.map((pet) => pet.petName),
        currentPetIndex,
        currentPet: pets[currentPetIndex],
        noPets: false,
      });
    } catch (error) {
      // request 已统一提示
    } finally {
      this.setData({ loading: false });
    }
  },

  onPetChange(event: any) {
    if (!this.manualEdit()) return;
    const index = Number(event.detail.value);
    this.setData({
      currentPetIndex: index,
      currentPet: this.data.pets[index] || null,
    });
  },

  onMoodTap(event: any) {
    if (!this.manualEdit()) return;
    this.setData({ moodIndex: Number(event.currentTarget.dataset.index) });
  },

  onCategoryTap(event: any) {
    if (!this.manualEdit()) return;
    this.setData({ categoryIndex: Number(event.currentTarget.dataset.index) });
  },

  onDateChange(event: any) {
    if (!this.manualEdit()) return;
    this.setData({ diaryDate: event.detail.value });
  },

  onContentInput(event: any) {
    if (!this.manualEdit()) return;
    this.setData({ content: event.detail.value });
  },

  onPublicChange(event: any) {
    if (!this.manualEdit()) return;
    this.setData({ isPublic: event.detail.value });
  },

  async choosePhotos(_event?: any, agent?: { taskId: string; actionId: string }) {
    if (!agent && agentWaitingForPhotos()) {
      await provideAgentPhotos();
      return;
    }
    if (this.data.uploading || this.data.saving || this.data.savedDiaryId) {
      return false;
    }
    if (!agent && !this.manualEdit()) return false;
    const remain = 9 - this.data.photos.length;
    if (remain <= 0) {
      wx.showToast({ title: '最多上传 9 张照片', icon: 'none' });
      return this.data.photos.length > 0;
    }

    this.setData({ uploading: true });
    try {
      const result = await new Promise<any>((resolve, reject) => {
        wx.chooseMedia({
          count: remain,
          mediaType: ['image'],
          sizeType: ['compressed'],
          success: resolve,
          fail: reject,
        });
      });

      const files = result.tempFiles || [];
      if (!files.length) {
        return false;
      }

      for (const file of files) {
        if (agent) assertAgentAction(agent.taskId, agent.actionId);
        const url = await uploadImage(file.tempFilePath, agent ? 'assistant' : 'diary');
        const previews = await resolveImages([url]);
        if (agent) assertAgentAction(agent.taskId, agent.actionId);
        this._agentRevision++;
        this.setData({ photos: this.data.photos.concat(url), photoPreviews: this.data.photoPreviews.concat(previews) });
      }
      return true;
    } catch (error) {
      // 用户取消或上传失败
      return false;
    } finally {
      this.setData({ uploading: false });
    }
  },

  removePhoto(event: any) {
    if (!this.manualEdit()) return;
    const index = Number(event.currentTarget.dataset.index);
    const photos = this.data.photos.slice();
    const photoPreviews = this.data.photoPreviews.slice();
    photos.splice(index, 1);
    photoPreviews.splice(index, 1);
    this.setData({ photos, photoPreviews });
  },

  goCreatePet() {
    wx.navigateTo({ url: '/pages/pet/edit' });
  },

  async onSave(_event?: any, agent?: { taskId: string; actionId: string }) {
    if (this.data.savedDiaryId) return this.data.savedDiaryId;
    if (!agent) takeOverAgent();
    const {
      currentPet,
      diaryDate,
      moodIndex,
      moods,
      categoryIndex,
      categories,
      content,
      photos,
      isPublic,
      saving,
    } = this.data;

    if (saving || this.data.uploading) {
      if (agent) throw new Error('日记正在保存或上传照片，请稍后继续');
      return;
    }
    if (!currentPet) {
      if (agent) throw new Error('请先选择宠物');
      wx.showToast({ title: '请先选择宠物', icon: 'none' });
      return;
    }
    if (!content.trim() && !photos.length) {
      if (agent) throw new Error('请填写日记内容或上传照片');
      wx.showToast({ title: '写点什么或上传一张照片吧', icon: 'none' });
      return;
    }

    this.setData({ saving: true });
    try {
      const payload = {
        petId: currentPet.id,
        diaryDate,
        category: categories[categoryIndex]?.value || 'daily',
        mood: moods[moodIndex]?.value || 'happy',
        content: content.trim(),
        photoUrls: photos,
        isPublic: isPublic ? 1 : 0,
      };
      let diaryId: string;
      if (agent) {
        assertAgentAction(agent.taskId, agent.actionId);
        diaryId = await saveAgentDiary(agent.taskId, agent.actionId, payload);
        this.setData({ savedDiaryId: diaryId });
      } else {
        diaryId = String(await createDiary(payload));
      }
      wx.showToast({ title: '记录成功', icon: 'success' });
      if (!agent) setTimeout(() => wx.navigateBack(), 500);
      return diaryId;
    } catch (error) {
      if (agent) throw error;
      // request 已统一提示
    } finally {
      this.setData({ saving: false });
    }
  },
});
