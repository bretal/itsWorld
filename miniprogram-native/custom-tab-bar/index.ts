import { assetUrl } from '../utils/assets';
import { resolveImage } from '../utils/image';

Component({
  data: {
    selected: 0,
    resolvedIcons: [] as string[],
    publishIcon: assetUrl('tabs/publish.png'),
    publishIconSrc: '',
    list: [
      {
        pagePath: '/pages/home/index',
        text: '首页',
        iconPath: assetUrl('tabs/home.png'),
      },
      {
        pagePath: '/pages/album/index',
        text: '相册',
        iconPath: assetUrl('tabs/album.png'),
      },
      {
        pagePath: '/pages/community/index',
        text: '社区',
        iconPath: assetUrl('tabs/community.png'),
      },
      {
        pagePath: '/pages/profile/index',
        text: '我的',
        iconPath: assetUrl('tabs/profile.png'),
      },
    ],
  },

  lifetimes: {
    attached() {
      Promise.all(
        this.data.list.map((item: any) => resolveImage(item.iconPath)),
      ).then((resolvedIcons) => {
        this.setData({ resolvedIcons });
      });
      resolveImage(this.data.publishIcon).then((publishIconSrc) => {
        this.setData({ publishIconSrc });
      });
    },
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
