import { withStaticImages } from '../../utils/static-images';
import { requireLogin } from '../../utils/auth';
import { deleteDiary, getDiary, likeDiary, unlikeDiary } from '../../services/diary';
import type { Diary } from '../../services/diary';
import { getPet } from '../../services/pet';
import type { Pet } from '../../services/pet';
import { DIARY_MOOD_LABELS, formatDate, formatDateTime } from '../../utils/format';
import { resolveImage, resolveImages } from '../../utils/image';

Page(withStaticImages({
  data: {
    id: 0,
    diary: null as Diary | null,
    pet: null as Pet | null,
    photos: [] as string[],
    dateText: '',
    moodLabel: '',
    categoryLabel: '',
    petAvatarSrc: '',
    liked: false,
    liking: false,
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
        liked: !!diary.liked,
        pet,
        petAvatarSrc,
        photos: await resolveImages(diary.photoUrls || []),
        categoryLabel: diary.category
          ? categoryLabels[diary.category] || diary.category
          : '',
        dateText: formatDateTime(diary.occurredAt || diary.createdAt, true) || formatDate(diary.diaryDate),
        moodLabel: diary.greeting ? DIARY_MOOD_LABELS[diary.greeting] || diary.greeting : '',
        loading: false,
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  async like() {
    const { diary, liked } = this.data;
    if (!diary || this.data.liking) {
      return;
    }

    try {
      this.setData({ liking: true });
      if (liked) await unlikeDiary(diary.id);
      else await likeDiary(diary.id);
      const updated = await getDiary(diary.id);
      this.setData({
        liked: !!updated.liked,
        diary: updated,
      });
    } catch (error) {
      // request 已统一提示
    } finally {
      this.setData({ liking: false });
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
}, ["pet-placeholder.png", "community/like.png", "community/comment.png"]));
