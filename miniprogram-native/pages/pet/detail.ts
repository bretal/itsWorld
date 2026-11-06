import { requireLogin } from '../../utils/auth';
import { getPet } from '../../services/pet';
import type { Pet } from '../../services/pet';
import { listDiariesPage } from '../../services/diary';
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
}

Page({
  data: {
    id: 0,
    pet: null as Pet | null,
    diaries: [] as DiaryItem[],
    activeTab: 'growth',
    tabs: [
      { key: 'growth', label: '成长' },
      { key: 'health', label: '健康' },
      { key: 'profile', label: '档案' },
    ],
    ageText: '',
    genderLabel: '',
    petTypeLabel: '',
    petAvatarSrc: '',
    loading: true,
    loadingMore: false,
    diaryPage: 1,
    diarySize: 10,
    diaryHasMore: true,
  },

  onLoad(options: any) {
    const id = Number(options?.id || 0);
    this.setData({ id });
  },

  onShow() {
    if (!requireLogin()) {
      return;
    }
    if (this.data.id) {
      this.loadData();
    }
  },

  onReachBottom() {
    if (
      this.data.activeTab === 'growth' &&
      this.data.diaryHasMore &&
      !this.data.loading &&
      !this.data.loadingMore
    ) {
      this.loadMoreDiaries();
    }
  },

  async loadData() {
    const { id, diarySize } = this.data;
    if (!id) {
      return;
    }

    try {
      const pet = await getPet(id);
      const diaryPage = await listDiariesPage(id, 1, diarySize).catch(() => null);
      const petAvatarSrc = await resolveImage(pet.avatarUrl);
      const petTypeMap: Record<string, string> = {
        dog: '狗狗',
        cat: '猫咪',
        other: '其他',
      };
      this.setData({
        pet,
        ageText: formatAge(pet.birthDate),
        genderLabel: genderText(pet.gender),
        petAvatarSrc,
        petTypeLabel: pet.petType
          ? petTypeMap[pet.petType] || pet.petType
          : '--',
        diaries: await this.mapDiaries(diaryPage?.records || []),
        diaryPage: diaryPage?.pageNum || 1,
        diaryHasMore: Boolean(
          diaryPage && diaryPage.pageNum < diaryPage.totalPages,
        ),
        loading: false,
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  async loadMoreDiaries() {
    const { id, diaryPage, diarySize } = this.data;
    if (!id) {
      return;
    }

    this.setData({ loadingMore: true });
    try {
      const result = await listDiariesPage(id, diaryPage + 1, diarySize);
      const items = await this.mapDiaries(result.records || []);
      this.setData({
        diaries: this.data.diaries.concat(items),
        diaryPage: result.pageNum,
        diaryHasMore: result.pageNum < result.totalPages,
        loadingMore: false,
      });
    } catch (error) {
      this.setData({ loadingMore: false });
    }
  },

  async mapDiaries(list: Diary[]): Promise<DiaryItem[]> {
    return Promise.all(
      (list || []).map(async (diary) => ({
        ...diary,
        dateText: formatDate(diary.diaryDate),
        timeText: formatDateTime(diary.createdAt),
        photos: await resolveImages(diary.photoUrls || []),
      })),
    );
  },

  switchTab(event: any) {
    this.setData({ activeTab: event.currentTarget.dataset.key });
  },

  goEdit() {
    wx.navigateTo({ url: `/pages/pet/edit?id=${this.data.id}` });
  },

  goRecord() {
    wx.navigateTo({ url: '/pages/diary/edit' });
  },

  goHealth() {
    wx.navigateTo({ url: '/pages/health/index' });
  },

  goDiaryDetail(event: any) {
    const diaryId = Number(event.currentTarget.dataset.id);
    if (!diaryId) {
      return;
    }
    wx.navigateTo({ url: `/pages/diary/detail?id=${diaryId}` });
  },
});
