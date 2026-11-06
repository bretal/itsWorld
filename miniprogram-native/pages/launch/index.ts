import { isLoggedIn } from '../../utils/auth';
import { assetUrl } from '../../utils/assets';
import { resolveImage } from '../../utils/image';

Page({
  data: {
    launchBg: '',
  },

  onLoad() {
    resolveImage(assetUrl('launch-bg.jpg')).then((launchBg) => {
      this.setData({ launchBg });
    });
  },

  onTapStart() {
    if (isLoggedIn()) {
      wx.reLaunch({ url: '/pages/home/index' });
      return;
    }

    const onboardingShown = wx.getStorageSync('onboarding_shown');
    wx.reLaunch({
      url: onboardingShown
        ? '/pages/auth/login'
        : '/pages/onboarding/index',
    });
  },
});
