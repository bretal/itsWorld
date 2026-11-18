import { request } from '../../utils/request';
import { withStaticImages } from '../../utils/static-images';
import { login } from '../../services/user';
import { setAuth } from '../../utils/auth';
import { assetUrl } from '../../utils/assets';
import { resolveImage } from '../../utils/image';

Page(withStaticImages({
  data: {
    username: '',
    password: '',
    logoUrl: '',
    registering: false,
    phone: '',
    confirmPassword: '',
    loading: false,
    showPassword: false,
  },

  onLoad() {
    resolveImage(assetUrl('logo.jpg')).then((logoUrl) => {
      this.setData({ logoUrl });
    });
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

  toggleRegistration() {
    this.setData({ registering: !this.data.registering, password: '', confirmPassword: '' });
  },

  onPhoneInput(event: any) {
    this.setData({ phone: event.detail.value });
  },

  onConfirmPasswordInput(event: any) {
    this.setData({ confirmPassword: event.detail.value });
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

    const { registering, phone, confirmPassword } = this.data;
    if (registering && (username.trim().length < 3 || !/^1[3-9]\d{9}$/.test(phone.trim())
      || password.length < 8 || password !== confirmPassword)) {
      wx.showToast({ title: '请检查用户名、手机号和两次密码', icon: 'none' });
      return;
    }

    this.setData({ loading: true });
    try {
      if (registering) {
        await request<number>({
          url: '/user/register',
          method: 'POST',
          data: { username: username.trim(), phone: phone.trim(), password, confirmPassword },
        });
        this.toggleRegistration();
        wx.showToast({ title: '注册成功，请登录', icon: 'none' });
        return;
      }
      const result = await login(username.trim(), password);
      setAuth(result.token, result.user);
      wx.reLaunch({ url: '/pages/home/index' });
    } catch (error) {
      // request 已统一提示
    } finally {
      this.setData({ loading: false });
    }
  },
}, ["avatar-placeholder.png"]));
