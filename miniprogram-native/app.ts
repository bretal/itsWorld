import { getUser } from './utils/auth';
import { setAgentForeground } from './utils/agent-runner';

App({
  onShow() { setAgentForeground(true); },
  onHide() { setAgentForeground(false); },
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
