import { isLoggedIn } from '../../utils/auth';
import { assetUrl } from '../../utils/assets';
import { resolveImage } from '../../utils/image';

Page({
  data: {
    launchBg: '',
    startUrl: '/pages/onboarding/index',
    startOpenType: 'redirect',
  },

  onLoad() {
    resolveImage(assetUrl('launch-bg.jpg')).then((launchBg) => {
      this.setData({ launchBg });
    });
  },

  onShow() {
    const loggedIn = isLoggedIn();
    this.setData({
      startUrl: loggedIn
        ? '/pages/home/index'
        : wx.getStorageSync('onboarding_shown')
          ? '/pages/auth/login'
          : '/pages/onboarding/index',
      startOpenType: loggedIn ? 'switchTab' : 'redirect',
    });
  },

  onTapStart() {
    console.info('启动页按钮已点击', this.data.startUrl);
  },

  onStartFail(event: { detail: unknown }) {
    console.warn('启动页跳转失败', event.detail);
    wx.showToast({ title: '页面打开失败，请重试', icon: 'none' });
  },
});
