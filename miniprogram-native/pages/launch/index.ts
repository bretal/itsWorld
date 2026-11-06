import { isLoggedIn } from '../../utils/auth';

Page({
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
