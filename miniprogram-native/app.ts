import { getUser } from './utils/auth';

App({
  globalData: {
    user: null,
    currentPetId: null,
  },

  onLaunch() {
    const user = getUser();
    if (user) {
      this.globalData.user = user;
    }

    const currentPetId = wx.getStorageSync('currentPetId');
    if (currentPetId) {
      this.globalData.currentPetId = Number(currentPetId);
    }
  },
});
