Page({
  data: {
    current: 0,
    slides: [
      {
        emoji: '📷',
        title: '记录宠物的\n每个瞬间',
        desc: '拍照、日记、成长相册\n留下它的可爱与独特',
        tint: '#FFF6DC',
      },
      {
        emoji: '💛',
        title: '每天一句问候\n读懂它的心情',
        desc: '位置、活动、心情和照片\n把陪伴变成看得见的记录',
        tint: '#EAF2FF',
      },
      {
        emoji: '🐾',
        title: '遇见同频的\n宠友',
        desc: '分享养宠日常，互相点赞\n一起把日子过得软乎乎',
        tint: '#FFEFF4',
      },
    ],
  },

  onSwiperChange(event: any) {
    this.setData({ current: event.detail.current });
  },

  onNext() {
    const { current, slides } = this.data;
    if (current < slides.length - 1) {
      this.setData({ current: current + 1 });
      return;
    }
    this.finish();
  },

  onSkip() {
    this.finish();
  },

  finish() {
    wx.setStorageSync('onboarding_shown', true);
    wx.reLaunch({ url: '/pages/auth/login' });
  },
});
