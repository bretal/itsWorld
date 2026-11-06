import {
  getToken,
  getUser,
  requireLogin,
  setAuth,
} from '../../utils/auth';
import { updateProfile } from '../../services/user';
import { uploadImage } from '../../utils/upload';
import { resolveImage } from '../../utils/image';

Page({
  data: {
    nickName: '',
    bio: '',
    region: '',
    genderIndex: 0,
    genders: ['未设置', '男', '女'],
    avatarUrl: '',
    avatarPreviewSrc: '',
    uploading: false,
    saving: false,
  },

  onLoad() {
    if (!requireLogin()) {
      return;
    }
    const user = getUser();
    if (!user) {
      return;
    }
    this.setData({
      nickName: user.nickName || '',
      bio: user.bio || '',
      region: user.region || '',
      genderIndex:
        user.gender === 1 || user.gender === 2 ? user.gender : 0,
      avatarUrl: user.avatarUrl || '',
    });
    resolveImage(user.avatarUrl).then((avatarPreviewSrc) => {
      this.setData({ avatarPreviewSrc });
    });
  },

  onInput(event: any) {
    const field = event.currentTarget.dataset.field;
    if (!field) {
      return;
    }
    this.setData({ [field]: event.detail.value });
  },

  onGenderChange(event: any) {
    this.setData({ genderIndex: Number(event.detail.value) });
  },

  async chooseAvatar() {
    if (this.data.uploading) {
      return;
    }
    try {
      const result = await new Promise<any>((resolve, reject) => {
        wx.chooseMedia({
          count: 1,
          mediaType: ['image'],
          sizeType: ['compressed'],
          success: resolve,
          fail: reject,
        });
      });

      const filePath = result.tempFiles?.[0]?.tempFilePath;
      if (!filePath) {
        return;
      }

      this.setData({ uploading: true });
      const url = await uploadImage(filePath, 'avatar');
      const avatarPreviewSrc = await resolveImage(url);
      this.setData({ avatarUrl: url, avatarPreviewSrc });
    } catch (error) {
      // 用户取消或上传失败
    } finally {
      this.setData({ uploading: false });
    }
  },

  async onSave() {
    const { nickName, bio, region, genderIndex, avatarUrl, saving } = this.data;
    if (saving) {
      return;
    }
    if (!nickName.trim()) {
      wx.showToast({ title: '请填写昵称', icon: 'none' });
      return;
    }

    this.setData({ saving: true });
    try {
      const updated = await updateProfile({
        nickName: nickName.trim(),
        bio: bio.trim(),
        region: region.trim(),
        gender: genderIndex,
        avatarUrl: avatarUrl.startsWith('http') ? avatarUrl : undefined,
      });
      setAuth(getToken(), updated);
      wx.showToast({ title: '保存成功', icon: 'success' });
      setTimeout(() => wx.navigateBack(), 500);
    } catch (error) {
      // request 已统一提示
    } finally {
      this.setData({ saving: false });
    }
  },
});
