import { clearAuth, getUser, requireLogin } from '../../utils/auth';
import type { AuthUser } from '../../utils/auth';
import { getProfile } from '../../services/user';
import { listPets } from '../../services/pet';
import { listDiaries } from '../../services/diary';
import { resolveImage } from '../../utils/image';

Page({
  data: {
    user: null as AuthUser | null,
    displayName: '宠物家长',
    avatarUrl: '/assets/avatar-placeholder.png',
    avatarSrc: '',
    petCount: 0,
    photoCount: 0,
    menu: [
      { key: 'pets', icon: '🐾', title: '我的宠物', desc: '管理宠物档案' },
      { key: 'album', icon: '🖼️', title: '成长相册', desc: '按时间回看成长' },
      { key: 'messages', icon: '🔔', title: '消息通知', desc: '赞和评论' },
      { key: 'favorites', icon: '⭐', title: '我的收藏', desc: '收藏的帖子' },
      { key: 'help', icon: '💬', title: '帮助与反馈', desc: '使用问题与建议' },
      { key: 'settings', icon: '⚙️', title: '设置', desc: '账号与隐私' },
    ],
  },

  onShow() {
    const tabBar = (this as any).getTabBar?.();
    if (tabBar) {
      tabBar.setData({ selected: 3 });
    }
    if (!requireLogin()) {
      return;
    }
    this.loadData();
  },

  async loadData() {
    const cached = getUser();
    if (cached) {
      this.applyUser(cached);
    }

    try {
      const [profile, pets] = await Promise.all([
        getProfile().catch(() => cached),
        listPets().catch(() => []),
      ]);

      const petCount = (pets || []).length;
      let photoCount = 0;

      const storedPetId = Number(wx.getStorageSync('currentPetId') || 0);
      const currentPet =
        (pets || []).find((pet) => pet.id === storedPetId) ||
        (pets || [])[0] ||
        null;

      if (currentPet) {
        const diaries = await listDiaries(currentPet.id).catch(() => []);
        photoCount = (diaries || []).reduce(
          (total, diary) => total + (diary.photoUrls || []).length,
          0,
        );
      }

      const user = profile || cached;
      if (user) {
        this.applyUser(user);
      }
      this.setData({
        petCount,
        photoCount,
      });
    } catch (error) {
      // request 已统一提示
    }
  },

  applyUser(user: AuthUser) {
    this.setData({
      user,
      displayName: user.nickName || user.username || '宠物家长',
      avatarUrl: user.avatarUrl || '/assets/avatar-placeholder.png',
    });
    resolveImage(user.avatarUrl).then((avatarSrc) => {
      this.setData({ avatarSrc });
    });
  },

  onTapMenu(event: any) {
    const key = event.currentTarget.dataset.key;
    if (key === 'pets') {
      wx.navigateTo({ url: '/pages/pet/list' });
      return;
    }
    if (key === 'album') {
      wx.switchTab({ url: '/pages/album/index' });
      return;
    }
    if (key === 'settings') {
      this.showSettings();
      return;
    }
    if (key === 'messages') {
      wx.showToast({ title: '消息通知接口待接入', icon: 'none' });
      return;
    }
    if (key === 'favorites') {
      wx.showToast({ title: '收藏功能后续上线', icon: 'none' });
      return;
    }
    wx.showToast({ title: '帮助与反馈后续上线', icon: 'none' });
  },

  goEditProfile() {
    wx.navigateTo({ url: '/pages/profile/edit' });
  },

  showSettings() {
    wx.showModal({
      title: '账号设置',
      content: '当前版本仅支持退出登录与查看关于信息。',
      confirmText: '退出登录',
      cancelText: '取消',
      success: (result: any) => {
        if (!result.confirm) {
          return;
        }
        wx.showModal({
          title: '确认退出',
          content: '退出后需要重新登录，确定吗？',
          success: (confirmResult: any) => {
            if (!confirmResult.confirm) {
              return;
            }
            clearAuth();
            wx.reLaunch({ url: '/pages/auth/login' });
          },
        });
      },
    });
  },

  onLogout() {
    this.showSettings();
  },
});
