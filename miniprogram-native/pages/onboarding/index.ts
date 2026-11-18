import { assetUrl } from '../../utils/assets';

Page({
  data: {
    current: 0,
    lastImageIndex: 0,
    topInset: 88,
    slides: [
      {
        image: assetUrl('miniapp-v1/onboarding/journal-v1.webp'),
        title: '记录宠物的\n每个瞬间',
        desc: '拍照、日记、成长相册\n留下它的可爱与独特',
      },
      {
        image: assetUrl('miniapp-v1/onboarding/check-in-v1.webp'),
        title: '每天一句问候\n读懂它的心情',
        desc: '位置、活动、心情和照片\n把陪伴变成看得见的记录',
      },
      {
        image: assetUrl('miniapp-v1/onboarding/community-v1.webp'),
        title: '遇见同频的\n宠友',
        desc: '分享养宠日常，互相点赞\n一起把日子过得软乎乎',
      },
    ],
  },

  onLoad() {
    const capsule = wx.getMenuButtonBoundingClientRect();
    if (capsule.bottom > 0) {
      this.setData({ topInset: capsule.bottom + 8 });
    }
  },

  onSwiperChange(event: any) {
    this.showSlide(event.detail.current);
  },

  onFirstImageReady() {
    if (this.data.lastImageIndex === 0) {
      this.setData({ lastImageIndex: 1 });
    }
  },

  showSlide(index: number) {
    const nextImageIndex = Math.min(index + 1, this.data.slides.length - 1);
    this.setData({
      current: index,
      lastImageIndex: Math.max(this.data.lastImageIndex, nextImageIndex),
    });
  },

  onNext() {
    const { current, slides } = this.data;
    if (current < slides.length - 1) {
      this.showSlide(current + 1);
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
