import { getUser, requireLogin } from '../../utils/auth';
import { listPets } from '../../services/pet';
import type { Pet } from '../../services/pet';
import { listDiariesPage } from '../../services/diary';
import type { Diary } from '../../services/diary';
import {
  formatAge,
  formatDateTime,
  genderText,
  todayString,
} from '../../utils/format';
import { resolveImage, resolveImages } from '../../utils/image';

interface HomePet extends Pet {
  ageText?: string;
  metaText?: string;
  avatarSrc?: string;
}

let loadSequence = 0;

Page({
  data: {
    statusBarHeight: 20,
    user: null as any,
    pets: [] as HomePet[],
    currentPet: null as HomePet | null,
    petAvatarSrc: '',
    todayDiary: null as Diary | null,
    todayPhotos: [] as string[],
    todayDiaryTime: '',
    loading: true,
    petPickerVisible: false,
  },

  onLoad() {
    const windowInfo = wx.getWindowInfo
      ? wx.getWindowInfo()
      : wx.getSystemInfoSync();
    this.setData({
      statusBarHeight: windowInfo.statusBarHeight || 20,
      user: getUser(),
    });
  },

  onShow() {
    const tabBar = (this as any).getTabBar?.();
    if (tabBar) {
      tabBar.setData({ selected: 0 });
    }
    if (!requireLogin()) {
      return;
    }
    this.loadData();
  },

  onPullDownRefresh() {
    this.loadData().finally(() => wx.stopPullDownRefresh());
  },

  async loadData() {
    const sequence = ++loadSequence;
    try {
      const pets = await listPets();
      if (sequence !== loadSequence) {
        return;
      }

      const displayPets: HomePet[] = await Promise.all(
        (pets || []).map(async (pet) => ({
          ...pet,
          ageText: formatAge(pet.birthDate),
          metaText: [
            formatAge(pet.birthDate),
            pet.breed || '',
            genderText(pet.gender),
          ]
            .filter(Boolean)
            .join(' · '),
          avatarSrc: await resolveImage(pet.avatarUrl),
        })),
      );

      const storedPetId = Number(wx.getStorageSync('currentPetId') || 0);
      let currentPet =
        displayPets.find((pet) => pet.id === storedPetId) ||
        displayPets[0] ||
        null;

      if (currentPet) {
        wx.setStorageSync('currentPetId', currentPet.id);
        try {
          const app = getApp<IAppOption>();
          app.globalData.currentPetId = currentPet.id;
        } catch (error) {
          // 忽略
        }
      }

      let todayDiary: Diary | null = null;
      if (currentPet) {
        const diaryPage = await listDiariesPage(currentPet.id, 1, 10).catch(
          () => null,
        );
        const diaries = diaryPage?.records || [];
        if (sequence !== loadSequence) {
          return;
        }
        const today = todayString();
        todayDiary =
          (diaries || []).find(
            (diary) => (diary.diaryDate || '').slice(0, 10) === today,
          ) ||
          (diaries || [])[0] ||
          null;
      }

      const todayPhotos = await resolveImages(todayDiary?.photoUrls || []);
      this.setData({
        pets: displayPets,
        currentPet,
        petAvatarSrc: currentPet?.avatarSrc || '',
        todayDiary,
        todayPhotos,
        todayDiaryTime: todayDiary?.createdAt
          ? formatDateTime(todayDiary.createdAt)
          : '',
        loading: false,
      });
    } catch (error) {
      if (sequence === loadSequence) {
        this.setData({ loading: false });
      }
    }
  },

  togglePetPicker() {
    if (!this.data.pets.length) {
      this.goCreatePet();
      return;
    }
    this.setData({ petPickerVisible: !this.data.petPickerVisible });
  },

  closePetPicker() {
    this.setData({ petPickerVisible: false });
  },

  noop() {
    // 阻止点击面板时关闭遮罩
  },

  async selectPet(event: any) {
    const petId = Number(event.currentTarget.dataset.id);
    const currentPet = this.data.pets.find((pet) => pet.id === petId);
    if (!currentPet) {
      return;
    }

    wx.setStorageSync('currentPetId', petId);
    try {
      const app = getApp<IAppOption>();
      app.globalData.currentPetId = petId;
    } catch (error) {
      // 忽略
    }

    this.setData({
      currentPet,
      petAvatarSrc: currentPet.avatarSrc || '',
      petPickerVisible: false,
      todayDiary: null,
      todayPhotos: [],
      todayDiaryTime: '',
    });

    const diaryPage = await listDiariesPage(petId, 1, 10).catch(() => null);
    const diaries = diaryPage?.records || [];
    const today = todayString();
    const todayDiary =
      (diaries || []).find(
        (diary) => (diary.diaryDate || '').slice(0, 10) === today,
      ) ||
      (diaries || [])[0] ||
      null;
    this.setData({
      todayDiary,
      todayPhotos: await resolveImages(todayDiary?.photoUrls || []),
      todayDiaryTime: todayDiary?.createdAt
        ? formatDateTime(todayDiary.createdAt)
        : '',
    });
  },

  goPetDetail() {
    if (!this.data.currentPet) {
      this.goCreatePet();
      return;
    }
    wx.navigateTo({
      url: `/pages/pet/detail?id=${this.data.currentPet.id}`,
    });
  },

  goCreatePet() {
    wx.navigateTo({ url: '/pages/pet/edit' });
  },

  goManagePets() {
    this.setData({ petPickerVisible: false });
    wx.navigateTo({ url: '/pages/pet/list' });
  },

  goAlbum() {
    wx.switchTab({ url: '/pages/album/index' });
  },

  goCommunity() {
    wx.switchTab({ url: '/pages/community/index' });
  },

  onTapFeature(event: any) {
    const key = event.currentTarget.dataset.key;
    if (key === 'ai') {
      wx.showToast({ title: 'AI 情绪识别后续上线', icon: 'none' });
      return;
    }
    if (key === 'health') {
      if (!this.data.currentPet) {
        this.goCreatePet();
        return;
      }
      wx.navigateTo({ url: '/pages/health/index' });
      return;
    }
    if (!this.data.currentPet) {
      this.goCreatePet();
      return;
    }
    this.goAlbum();
  },

  onTapCamera() {
    wx.showToast({ title: 'AI 情绪识别后续上线', icon: 'none' });
  },

  goRecordDiary() {
    if (!this.data.currentPet) {
      this.goCreatePet();
      return;
    }
    wx.navigateTo({ url: '/pages/diary/edit' });
  },

  goTodayDiary() {
    if (!this.data.todayDiary) {
      this.goRecordDiary();
      return;
    }
    wx.navigateTo({
      url: `/pages/diary/detail?id=${this.data.todayDiary.id}`,
    });
  },
});
