import { requireLogin } from '../../utils/auth';
import { deleteDiary, getDiary, likeDiary } from '../../services/diary';
import type { Diary } from '../../services/diary';
import { getPet } from '../../services/pet';
import type { Pet } from '../../services/pet';
import { formatDate, formatDateTime } from '../../utils/format';
import { resolveImage, resolveImages } from '../../utils/image';

Page({
  data: {
    id: 0,
    diary: null as Diary | null,
    pet: null as Pet | null,
    photos: [] as string[],
    dateText: '',
    timeText: '',
    categoryLabel: '',
    petAvatarSrc: '',
    liked: false,
    loading: true,
  },

  onLoad(options: any) {
    if (!requireLogin()) {
      return;
    }
    const id = Number(options?.id || 0);
    this.setData({ id });
    if (id) {
      this.loadData(id);
    }
  },

  async loadData(id: number) {
    try {
      const diary = await getDiary(id);
      const pet = diary.petId
        ? await getPet(diary.petId).catch(() => null)
        : null;
      const categoryLabels: Record<string, string> = {
        daily: '日常',
        food: '饮食',
        health: '健康',
      };
      const petAvatarSrc = await resolveImage(pet?.avatarUrl);
      this.setData({
        diary,
        pet,
        petAvatarSrc,
        photos: await resolveImages(diary.photoUrls || []),
        categoryLabel: diary.category
          ? categoryLabels[diary.category] || diary.category
          : '',
        dateText: formatDate(diary.diaryDate),
        timeText: formatDateTime(diary.createdAt),
        loading: false,
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  async like() {
    const { diary, liked } = this.data;
    if (!diary || liked) {
      return;
    }

    try {
      await likeDiary(diary.id);
      this.setData({
        liked: true,
        diary: {
          ...diary,
          likeCount: (diary.likeCount || 0) + 1,
        },
      });
    } catch (error) {
      // request 已统一提示
    }
  },

  remove() {
    const { diary } = this.data;
    if (!diary) {
      return;
    }

    wx.showModal({
      title: '删除日记',
      content: '删除后不可恢复，确定删除这篇日记吗？',
      confirmText: '删除',
      confirmColor: '#e05b6f',
      success: async (result: any) => {
        if (!result.confirm) {
          return;
        }
        try {
          await deleteDiary(diary.id);
          wx.showToast({ title: '已删除', icon: 'success' });
          setTimeout(() => wx.navigateBack(), 500);
        } catch (error) {
          // request 已统一提示
        }
      },
    });
  },
});
