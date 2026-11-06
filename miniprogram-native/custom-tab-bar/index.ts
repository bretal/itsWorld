Component({
  data: {
    selected: 0,
    list: [
      {
        pagePath: '/pages/home/index',
        text: '首页',
        iconPath: '/assets/tabs/home.png',
        selectedIconPath: '/assets/tabs/home-active.png',
      },
      {
        pagePath: '/pages/album/index',
        text: '相册',
        iconPath: '/assets/tabs/album.png',
        selectedIconPath: '/assets/tabs/album-active.png',
      },
      {
        pagePath: '/pages/community/index',
        text: '社区',
        iconPath: '/assets/tabs/community.png',
        selectedIconPath: '/assets/tabs/community-active.png',
      },
      {
        pagePath: '/pages/profile/index',
        text: '我的',
        iconPath: '/assets/tabs/profile.png',
        selectedIconPath: '/assets/tabs/profile-active.png',
      },
    ],
  },

  methods: {
    switchTab(event: any) {
      const { path, index } = event.currentTarget.dataset;
      wx.switchTab({ url: path });
      this.setData({ selected: Number(index) });
    },

    onPublish() {
      wx.showActionSheet({
        itemList: ['记录日记', '发帖子', '拍照识情绪（即将上线）'],
        success: (result: any) => {
          if (result.tapIndex === 0) {
            wx.navigateTo({ url: '/pages/diary/edit' });
            return;
          }
          if (result.tapIndex === 1) {
            wx.navigateTo({ url: '/pages/community/create' });
            return;
          }
          wx.showToast({ title: 'AI 情绪识别后续上线', icon: 'none' });
        },
      });
    },
  },
});
