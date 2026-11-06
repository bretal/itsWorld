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
}

Page({
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
      const pets = await listPets();
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
      this.setData({ currentPet, petAvatarSrc });
      await this.loadDiaries(reset);
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  async loadDiaries(reset: boolean) {
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
          timeText: formatDateTime(diary.createdAt),
          photos: await resolveImages(diary.photoUrls || []),
          liked: false,
        })),
      );

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
      this.setData({ loading: false, loadingMore: false });
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
    wx.navigateTo({ url: '/pages/diary/edit' });
  },

  goCreatePet() {
    wx.navigateTo({ url: '/pages/pet/edit' });
  },

  goDetail(event: any) {
    const id = Number(event.currentTarget.dataset.id);
    if (!id) {
      return;
    }
    wx.navigateTo({ url: `/pages/diary/detail?id=${id}` });
  },
});
