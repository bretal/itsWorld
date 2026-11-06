import { login } from '../../services/user';
import { setAuth } from '../../utils/auth';

Page({
  data: {
    username: '',
    password: '',
    loading: false,
    showPassword: false,
  },

  onUsernameInput(event: any) {
    this.setData({ username: event.detail.value });
  },

  onPasswordInput(event: any) {
    this.setData({ password: event.detail.value });
  },

  togglePassword() {
    this.setData({ showPassword: !this.data.showPassword });
  },

  async onSubmit() {
    const { username, password, loading } = this.data;
    if (loading) {
      return;
    }
    if (!username.trim()) {
      wx.showToast({ title: '请输入用户名、手机号或邮箱', icon: 'none' });
      return;
    }
    if (!password) {
      wx.showToast({ title: '请输入密码', icon: 'none' });
      return;
    }

    this.setData({ loading: true });
    try {
      const result = await login(username.trim(), password);
      setAuth(result.token, result.user);
      wx.reLaunch({ url: '/pages/home/index' });
    } catch (error) {
      // request 已统一提示
    } finally {
      this.setData({ loading: false });
    }
  },
});
