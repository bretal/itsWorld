import { uploadImage } from '../../utils/upload';
import { todayString } from '../../utils/format';
import type { AlbumDraft } from '../../services/assistant';

Component({
  properties: {
    draft: { type: Object, value: null },
    pets: { type: Array, value: [] },
    busy: { type: Boolean, value: false },
  },
  data: {
    noPets: false, petNames: [] as string[], currentPetIndex: 0, diaryDate: '', today: todayString(),
    moods: [
      { value: 'happy', label: '开心', emoji: '😊' },
      { value: 'calm', label: '平静', emoji: '😌' },
      { value: 'tired', label: '有点累', emoji: '😴' },
      { value: 'sad', label: '不开心', emoji: '😢' },
    ],
    categories: [
      { value: 'daily', label: '日常' }, { value: 'food', label: '饮食' }, { value: 'health', label: '健康' },
    ],
    moodIndex: 0, categoryIndex: 0, content: '', photos: [] as string[],
    photoPreviews: [] as string[], isPublic: false, saving: false, uploading: false,
  },
  observers: {
    'draft, pets, busy'(draft: AlbumDraft | null, pets: { id: number; label: string }[], busy: boolean) {
      if (!draft) return;
      this.setData({
        petNames: pets.map(pet => pet.label), currentPetIndex: Math.max(0, pets.findIndex(pet => pet.id === draft.petId)),
        noPets: !pets.length, diaryDate: draft.occurredAt.slice(0, 10), content: draft.content,
        photos: draft.photoUrls, photoPreviews: draft.photoUrls, isPublic: draft.isPublic === 1,
        moodIndex: Math.max(0, this.data.moods.findIndex(item => item.value === draft.mood)),
        categoryIndex: Math.max(0, this.data.categories.findIndex(item => item.value === draft.category)),
        saving: busy,
      });
    },
  },
  methods: {
    change(patch: Partial<AlbumDraft>) {
      this.triggerEvent('change', { ...this.properties.draft, ...patch });
    },
    onPetChange(event: { detail: { value: string } }) {
      if (this.properties.busy) return;
      const pet = this.properties.pets[Number(event.detail.value)];
      if (pet) this.change({ petId: pet.id });
    },
    onDateChange(event: { detail: { value: string } }) {
      if (!this.properties.busy) this.change({
        occurredAt: event.detail.value + this.properties.draft.occurredAt.slice(10),
      });
    },
    onMoodTap(event: { currentTarget: { dataset: { index: number } } }) {
      if (!this.properties.busy) this.change({ mood: this.data.moods[event.currentTarget.dataset.index].value });
    },
    onCategoryTap(event: { currentTarget: { dataset: { index: number } } }) {
      if (!this.properties.busy) this.change({ category: this.data.categories[event.currentTarget.dataset.index].value });
    },
    onContentInput(event: { detail: { value: string } }) {
      if (!this.properties.busy) this.change({ content: event.detail.value });
    },
    onPublicChange(event: { detail: { value: boolean } }) {
      if (!this.properties.busy) this.change({ isPublic: event.detail.value ? 1 : 0 });
    },
    removePhoto(event: { currentTarget: { dataset: { index: number } } }) {
      if (this.properties.busy || this.data.uploading) return;
      this.change({ photoUrls: this.data.photos.filter((_, index) => index !== event.currentTarget.dataset.index) });
    },
    async choosePhotos() {
      if (this.properties.busy || this.data.uploading) return;
      this.setData({ uploading: true });
      this.triggerEvent('uploadstate', true);
      try {
        const selection = await new Promise<{ tempFiles: { tempFilePath: string }[] }>((resolve, reject) =>
          wx.chooseMedia({ count: 9 - this.data.photos.length, mediaType: ['image'], success: resolve, fail: reject }));
        const photos = [...this.properties.draft.photoUrls];
        for (const file of selection.tempFiles) {
          const url = await uploadImage(file.tempFilePath, 'assistant');
          photos.push(url);
          this.change({ photoUrls: [...photos] });
        }
      } catch (_) {
        // 上传错误由公共上传工具显示，取消选择保留已有照片。
      } finally {
        this.setData({ uploading: false });
        this.triggerEvent('uploadstate', false);
      }
    },
    onSave() {
      if (!this.properties.busy && !this.data.uploading) this.triggerEvent('save');
    },
    goCreatePet() { wx.navigateTo({ url: '/pages/pet/edit' }); },
  },
});
