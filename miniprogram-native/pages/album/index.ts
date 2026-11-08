import { withStaticImages } from '../../utils/static-images';
import { requireLogin } from '../../utils/auth';
import { listPets } from '../../services/pet';
import type { Pet } from '../../services/pet';
import { likeDiary, listDiariesPage } from '../../services/diary';
import type { Diary } from '../../services/diary';
import {
  formatAge,
  formatDate,
  formatDateTime,
  genderText,
} from '../../utils/format';
import { resolveImage, resolveImages } from '../../utils/image';

interface DiaryItem extends Diary {
  dateText?: string;
  timeText?: string;
  photos: string[];
  liked?: boolean;
}

interface AlbumPet extends Pet {
  metaText?: string;
  avatarSrc?: string;
}

Page(withStaticImages({
  diaryRequest: 0,
  data: {
    tabs: [
      { key: 'all', label: '全部' },
      { key: 'food', label: '饮食' },
      { key: 'health', label: '健康' },
      { key: 'daily', label: '日常' },
    ],
    activeTab: 'all',
    diaries: [] as DiaryItem[],
    currentPet: null as AlbumPet | null,
    petAvatarSrc: '',
    loading: true,
    loadingMore: false,
    page: 1,
    size: 10,
    total: 0,
    hasMore: true,
    emptyHint: '',
    pets: [] as AlbumPet[],
    showPetSwitcher: false,
  },

  onShow() {
    const tabBar = (this as any).getTabBar?.();
    if (tabBar) {
      tabBar.setData({ selected: 1 });
    }
    if (!requireLogin()) {
      return;
    }
    this.loadData(true);
  },

  onPullDownRefresh() {
    this.loadData(true).finally(() => wx.stopPullDownRefresh());
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading && !this.data.loadingMore) {
      this.loadDiaries(false);
    }
  },

  async loadData(reset: boolean) {
    this.setData({ loading: reset });
    try {
      const pets: AlbumPet[] = await Promise.all((await listPets()).map(async pet => ({
        ...pet,
        avatarSrc: await resolveImage(pet.avatarUrl),
        metaText: [formatAge(pet.birthDate), pet.breed || pet.petType, genderText(pet.gender)].filter(Boolean).join(' · '),
      })));
      const storedPetId = Number(wx.getStorageSync('currentPetId') || 0);
      const pet =
        (pets || []).find((item) => item.id === storedPetId) ||
        (pets || [])[0] ||
        null;

      const currentPet: AlbumPet | null = pet
        ? {
            ...pet,
            metaText: [
              formatAge(pet.birthDate),
              pet.breed || '',
              genderText(pet.gender),
            ]
              .filter(Boolean)
              .join(' · '),
          }
        : null;

      const petAvatarSrc = await resolveImage(currentPet?.avatarUrl);
      if (currentPet) wx.setStorageSync('currentPetId', currentPet.id);
      this.setData({ pets, currentPet, petAvatarSrc, diaries: [], page: 1, hasMore: Boolean(currentPet) });
      await this.loadDiaries(reset);
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  async loadDiaries(reset: boolean) {
    const request = ++this.diaryRequest;
    const { currentPet, activeTab, size } = this.data;
    if (!currentPet) {
      this.setData({ diaries: [], loading: false, loadingMore: false });
      return;
    }

    const page = reset ? 1 : this.data.page + 1;
    if (reset) {
      this.setData({ loading: true });
    } else {
      this.setData({ loadingMore: true });
    }

    try {
      const result = await listDiariesPage(
        currentPet.id,
        page,
        size,
        activeTab === 'all' ? undefined : activeTab,
      );
      const items: DiaryItem[] = await Promise.all(
        (result.records || []).map(async (diary) => ({
          ...diary,
          dateText: formatDate(diary.diaryDate),
          timeText: (diary.createdAt || '').replace('T', ' ').slice(11, 16),
          moodText: ({ happy: '开心', calm: '平静', tired: '疲惫', sad: '难过' } as Record<string, string>)[diary.greeting || ''] || diary.greeting || '今日记录',
          photos: await resolveImages(diary.photoUrls || []),
          liked: false,
        })),
      );

      if (request !== this.diaryRequest) return;
      this.setData({
        diaries: reset ? items : this.data.diaries.concat(items),
        page: result.pageNum,
        total: result.total,
        hasMore: result.pageNum < result.totalPages,
        loading: false,
        loadingMore: false,
        emptyHint:
          reset && !items.length && activeTab !== 'all'
            ? '该分类暂无记录'
            : '',
      });
    } catch (error) {
      if (request === this.diaryRequest) this.setData({ loading: false, loadingMore: false });
    }
  },

  switchTab(event: any) {
    const key = event.currentTarget.dataset.key;
    if (key === this.data.activeTab) {
      return;
    }
    this.setData({ activeTab: key, diaries: [], hasMore: true });
    this.loadDiaries(true);
  },

  async like(event: any) {
    const index = Number(event.currentTarget.dataset.index);
    const item = this.data.diaries[index];
    if (!item || item.liked) {
      return;
    }

    try {
      await likeDiary(item.id);
      if (this.data.diaries[index]?.id !== item.id) return;
      const diaries = this.data.diaries.slice();
      const target = diaries[index];
      target.likeCount = (target.likeCount || 0) + 1;
      target.liked = true;
      this.setData({ diaries });
    } catch (error) {
      // request 已统一提示
    }
  },

  goRecord() {
    if (!this.data.currentPet) { this.goCreatePet(); return; }
    wx.setStorageSync('currentPetId', this.data.currentPet.id);
    wx.navigateTo({ url: '/pages/diary/edit' });
  },

  goCreatePet() {
    this.setData({ showPetSwitcher: false });
    wx.navigateTo({ url: '/pages/pet/edit' });
  },

  openPetSwitcher() { this.setData({ showPetSwitcher: true }); },
  closePetSwitcher() { this.setData({ showPetSwitcher: false }); },
  stopTap() {},
  selectPet(event: any) {
    const pet = this.data.pets.find((item: AlbumPet) => item.id === Number(event.currentTarget.dataset.id));
    if (!pet) return;
    this.setData({ showPetSwitcher: false });
    if (this.data.currentPet?.id === pet.id) return;
    wx.setStorageSync('currentPetId', pet.id);
    this.setData({ currentPet: pet, petAvatarSrc: pet.avatarSrc || '', diaries: [], page: 1, hasMore: true });
    wx.pageScrollTo({ scrollTop: 0, duration: 0 });
    this.loadDiaries(true);
  },

  goDetail(event: any) {
    const id = Number(event.currentTarget.dataset.id);
    if (!id) {
      return;
    }
    wx.navigateTo({ url: `/pages/diary/detail?id=${id}` });
  },
}, ["community/comment.png", "community/like.png", "pet-placeholder.png"]));
