import { withStaticImages } from '../../utils/static-images';
import { requireLogin } from '../../utils/auth';
import { deletePet, listPetsPage } from '../../services/pet';
import type { Pet } from '../../services/pet';
import { formatAge, genderText } from '../../utils/format';
import { resolveImage } from '../../utils/image';

interface PetItem extends Pet {
  metaText?: string;
  current?: boolean;
  avatarSrc?: string;
}

Page(withStaticImages({
  data: {
    pets: [] as PetItem[],
    loading: true,
    loadingMore: false,
    currentPetId: 0,
    page: 1,
    size: 10,
    total: 0,
    hasMore: true,
    keyword: '',
  },

  onShow() {
    if (!requireLogin()) {
      return;
    }
    this.loadData(true);
  },

  onPullDownRefresh() {
    this.loadData(true).finally(() => wx.stopPullDownRefresh());
  },

  onReachBottom() {
    this.loadMore();
  },

  onKeywordInput(event: any) {
    this.setData({ keyword: event.detail.value });
  },

  onSearch() {
    this.loadData(true);
  },

  onClearSearch() {
    this.setData({ keyword: '' });
    this.loadData(true);
  },

  async loadMore() {
    if (!this.data.hasMore || this.data.loading || this.data.loadingMore) {
      return;
    }
    await this.loadData(false);
  },

  async loadData(reset: boolean) {
    const page = reset ? 1 : this.data.page + 1;
    if (reset) {
      this.setData({ loading: true });
    } else {
      this.setData({ loadingMore: true });
    }

    try {
      const result = await listPetsPage(page, this.data.size, this.data.keyword);
      const currentPetId = Number(wx.getStorageSync('currentPetId') || 0);
      const displayPets = await Promise.all(
        (result.records || []).map(async (pet) => ({
          ...pet,
          metaText: [
            formatAge(pet.birthDate),
            pet.breed || '',
            genderText(pet.gender),
          ]
            .filter(Boolean)
            .join(' · '),
          current: pet.id === currentPetId,
          avatarSrc: await resolveImage(pet.avatarUrl),
        })),
      );

      this.setData({
        pets: reset ? displayPets : this.data.pets.concat(displayPets),
        currentPetId,
        page: result.pageNum,
        total: result.total,
        hasMore: result.pageNum < result.totalPages,
        loading: false,
        loadingMore: false,
      });
    } catch (error) {
      this.setData({ loading: false, loadingMore: false });
    }
  },

  goAdd() {
    wx.navigateTo({ url: '/pages/pet/edit' });
  },

  goDetail(event: any) {
    const id = Number(event.currentTarget.dataset.id);
    if (!id) {
      return;
    }
    wx.navigateTo({ url: `/pages/pet/detail?id=${id}` });
  },

  goEdit(event: any) {
    const id = Number(event.currentTarget.dataset.id);
    if (!id) {
      return;
    }
    wx.navigateTo({ url: `/pages/pet/edit?id=${id}` });
  },

  setCurrent(event: any) {
    const id = Number(event.currentTarget.dataset.id);
    if (!id) {
      return;
    }
    wx.setStorageSync('currentPetId', id);
    try {
      const app = getApp<IAppOption>();
      app.globalData.currentPetId = id;
    } catch (error) {
      // 忽略
    }
    this.setData({
      currentPetId: id,
      pets: this.data.pets.map((pet) => ({
        ...pet,
        current: pet.id === id,
      })),
    });
    wx.showToast({ title: '已设为当前宠物', icon: 'success' });
  },

  remove(event: any) {
    const id = Number(event.currentTarget.dataset.id);
    const pet = this.data.pets.find((item) => item.id === id);
    if (!pet) {
      return;
    }

    wx.showModal({
      title: '删除宠物',
      content: `确定删除「${pet.petName}」的档案吗？该操作不可恢复。`,
      confirmText: '删除',
      confirmColor: '#e05b6f',
      success: async (result: any) => {
        if (!result.confirm) {
          return;
        }
        try {
          await deletePet(id);
          if (this.data.currentPetId === id) {
            wx.removeStorageSync('currentPetId');
          }
          wx.showToast({ title: '已删除', icon: 'success' });
          this.loadData(true);
        } catch (error) {
          // request 已统一提示
        }
      },
    });
  },
}, ["pet-placeholder.png"]));
