import { AppConfig } from '@tarojs/taro';

const config: AppConfig = {
  pages: [
    'pages/home/index',
    'pages/diary/timeline',
    'pages/diary/greeting',
    'pages/health/index',
    'pages/community/index',
    'pages/ai/index',
    'pages/pet/list',
    'pages/pet/create',
    'pages/pet/detail',
    'pages/auth/login',
    'pages/auth/register',
    'pages/profile/index',
    'pages/profile/edit',
    'pages/profile/changePassword',
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#fff',
    navigationBarTitleText: '它的世界',
    navigationBarTextStyle: 'black',
  },
  tabBar: {
    color: '#999',
    selectedColor: '#1890ff',
    backgroundColor: '#fafafa',
    borderStyle: 'white',
    list: [
      {
        pagePath: 'pages/home/index',
        text: '首页',
        iconPath: 'assets/tabs/home.png',
        selectedIconPath: 'assets/tabs/home-active.png',
      },
      {
        pagePath: 'pages/diary/timeline',
        text: '日记',
        iconPath: 'assets/tabs/diary.png',
        selectedIconPath: 'assets/tabs/diary-active.png',
      },
      {
        pagePath: 'pages/health/index',
        text: '健康',
        iconPath: 'assets/tabs/health.png',
        selectedIconPath: 'assets/tabs/health-active.png',
      },
      {
        pagePath: 'pages/community/index',
        text: '社区',
        iconPath: 'assets/tabs/community.png',
        selectedIconPath: 'assets/tabs/community-active.png',
      },
      {
        pagePath: 'pages/profile/index',
        text: '我的',
        iconPath: 'assets/tabs/profile.png',
        selectedIconPath: 'assets/tabs/profile-active.png',
      },
    ],
  },
};

export default config;
