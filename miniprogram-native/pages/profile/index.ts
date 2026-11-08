import { withStaticImages } from '../../utils/static-images';
import { clearAuth, getUser, requireLogin } from '../../utils/auth';
import type { AuthUser } from '../../utils/auth';
import { getProfile } from '../../services/user';
import { listPets } from '../../services/pet';
import { listDiaries } from '../../services/diary';
import { resolveImage } from '../../utils/image';
import { assetUrl } from '../../utils/assets';

Page(withStaticImages({
  data: {
    user: null as AuthUser | null,
    displayName: '宠物家长',
    avatarUrl: '',
    avatarSrc: '',
    petCount: 0,
    photoCount: 0,
    petBgSrc: '',
    albumBgSrc: '',
    serviceMenu: [] as any[],
    utilityMenu: [] as any[],
    menu: [
      { key: 'member', image: 'profile-menu/member-crown.png', title: '会员中心' },
      { key: 'orders', image: 'profile-menu/order-list.png', title: '我的订单' },
      { key: 'bookings', image: 'profile-menu/booking-calendar.png', title: '我的预约' },
      { key: 'packages', image: 'profile-menu/custom-gift.png', title: '我的套餐' },
      { key: 'after-sales', image: 'profile-menu/delivery-box.png', title: '我的售后' },
      { key: 'cart', image: 'profile-menu/shopping-cart.png', title: '购物车' },
      { key: 'addresses', image: 'profile-menu/map-location.png', title: '收货地址' },
      { key: 'messages', image: 'profile-menu/notification-bell.png', title: '消息通知', dot: true },
      { key: 'favorites', image: 'profile-menu/favorite-heart.png', title: '我的收藏' },
      { key: 'help', image: 'profile-menu/chat.png', title: '帮助与反馈' },
      { key: 'settings', image: 'profile-menu/member-shield.png', title: '设置' },
    ],
  },

  onLoad() {
    const serviceKeys = ['orders', 'bookings', 'packages', 'after-sales', 'cart'];
    const utilityKeys = ['addresses', 'messages', 'favorites', 'help'];
    this.setData({
      serviceMenu: this.data.menu.filter((item: any) => serviceKeys.includes(item.key)),
      utilityMenu: this.data.menu.filter((item: any) => utilityKeys.includes(item.key)),
    });
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

  async loadBackgrounds() {
    const [petBgSrc, albumBgSrc] = await Promise.all([
      resolveImage(assetUrl('profile/pet-bg.jpg')),
      resolveImage(assetUrl('profile/album-bg.jpg')),
    ]);
    this.setData({ petBgSrc, albumBgSrc });
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
      avatarUrl: user.avatarUrl || '',
    });
    resolveImage(user.avatarUrl).then((avatarSrc) => {
      this.setData({ avatarSrc });
    });
  },

  onTapMenu(event: any) {
    const key = event.currentTarget.dataset.key;
    if (key === 'member') {
      wx.navigateTo({ url: '/pages/member/index' });
      return;
    }
    if (key === 'pets') {
      wx.navigateTo({ url: '/pages/pet/list' });
      return;
    }
    if (key === 'album') {
      wx.switchTab({ url: '/pages/album/index' });
      return;
    }
    if (key === 'orders') {
      wx.navigateTo({ url: '/pages/commerce/order/index' });
      return;
    }
    if (key === 'bookings') {
      wx.navigateTo({ url: '/pages/commerce/booking/list' });
      return;
    }
    if (key === 'packages') {
      wx.navigateTo({ url: '/pages/commerce/package/mine' });
      return;
    }
    if (key === 'after-sales') {
      wx.navigateTo({ url: '/pages/commerce/after-sale/list' });
      return;
    }
    if (key === 'cart') {
      wx.navigateTo({ url: '/pages/commerce/cart/index' });
      return;
    }
    if (key === 'addresses') {
      wx.navigateTo({ url: '/pages/commerce/address/index' });
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
}, ["member/membership-paw.png", "profile-menu/album.png", "profile-menu/booking-calendar.png", "profile-menu/chat.png", "profile-menu/custom-gift.png", "profile-menu/delivery-box.png", "profile-menu/favorite-heart.png", "profile-menu/map-location.png", "profile-menu/member-crown.png", "profile-menu/member-shield.png", "profile-menu/notification-bell.png", "profile-menu/order-list.png", "profile-menu/shopping-cart.png"]));
