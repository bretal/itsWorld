import { requireLogin } from '../../utils/auth';
import { listPets } from '../../services/pet';
import type { Pet } from '../../services/pet';
import { createDiary } from '../../services/diary';
import { uploadImage } from '../../utils/upload';
import { todayString } from '../../utils/format';
import { resolveImages } from '../../utils/image';

Page({
  data: {
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

  onShow() {
    // 首次进入，或从「添加宠物」返回后重新加载
    if (!this.data.pets.length) {
      this.loadPets();
    }
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
    }
  },

  onPetChange(event: any) {
    const index = Number(event.detail.value);
    this.setData({
      currentPetIndex: index,
      currentPet: this.data.pets[index] || null,
    });
  },

  onMoodTap(event: any) {
    this.setData({ moodIndex: Number(event.currentTarget.dataset.index) });
  },

  onCategoryTap(event: any) {
    this.setData({ categoryIndex: Number(event.currentTarget.dataset.index) });
  },

  onDateChange(event: any) {
    this.setData({ diaryDate: event.detail.value });
  },

  onContentInput(event: any) {
    this.setData({ content: event.detail.value });
  },

  onPublicChange(event: any) {
    this.setData({ isPublic: event.detail.value });
  },

  async choosePhotos() {
    if (this.data.uploading) {
      return;
    }
    const remain = 9 - this.data.photos.length;
    if (remain <= 0) {
      wx.showToast({ title: '最多上传 9 张照片', icon: 'none' });
      return;
    }

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
        return;
      }

      this.setData({ uploading: true });
      const uploaded: string[] = [];
      for (const file of files) {
        const url = await uploadImage(file.tempFilePath, 'diary');
        uploaded.push(url);
      }
      const photoPreviews = await resolveImages(uploaded);
      this.setData({
        photos: this.data.photos.concat(uploaded),
        photoPreviews: this.data.photoPreviews.concat(photoPreviews),
      });
    } catch (error) {
      // 用户取消或上传失败
    } finally {
      this.setData({ uploading: false });
    }
  },

  removePhoto(event: any) {
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

  async onSave() {
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

    if (saving) {
      return;
    }
    if (!currentPet) {
      wx.showToast({ title: '请先选择宠物', icon: 'none' });
      return;
    }
    if (!content.trim() && !photos.length) {
      wx.showToast({ title: '写点什么或上传一张照片吧', icon: 'none' });
      return;
    }

    this.setData({ saving: true });
    try {
      await createDiary({
        petId: currentPet.id,
        diaryDate,
        category: categories[categoryIndex]?.value || 'daily',
        mood: moods[moodIndex]?.value || 'happy',
        content: content.trim(),
        photoUrls: photos,
        isPublic: isPublic ? 1 : 0,
      });
      wx.showToast({ title: '记录成功', icon: 'success' });
      setTimeout(() => wx.navigateBack(), 500);
    } catch (error) {
      // request 已统一提示
    } finally {
      this.setData({ saving: false });
    }
  },
});
