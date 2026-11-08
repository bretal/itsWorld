import { withStaticImages } from '../../utils/static-images';
import { requireLogin } from '../../utils/auth';
import { listPets } from '../../services/pet';
import type { Pet } from '../../services/pet';
import { createPost } from '../../services/community';
import { uploadImage } from '../../utils/upload';
import { resolveImage, resolveImages } from '../../utils/image';

Page(withStaticImages({
  data: {
    currentPet: null as Pet | null,
    petAvatarSrc: '',
    title: '',
    content: '',
    photos: [] as string[],
    photoPreviews: [] as string[],
    uploading: false,
    saving: false,
  },

  onLoad() {
    if (!requireLogin()) {
      return;
    }
    this.loadCurrentPet();
  },

  async loadCurrentPet() {
    try {
      const pets = await listPets();
      const storedPetId = Number(wx.getStorageSync('currentPetId') || 0);
      const currentPet =
        (pets || []).find((pet) => pet.id === storedPetId) ||
        (pets || [])[0] ||
        null;
      this.setData({
        currentPet,
        petAvatarSrc: await resolveImage(currentPet?.avatarUrl),
      });
    } catch (error) {
      // request 已统一提示
    }
  },

  onTitleInput(event: any) {
    this.setData({ title: event.detail.value });
  },

  onContentInput(event: any) {
    this.setData({ content: event.detail.value });
  },

  async choosePhotos() {
    if (this.data.uploading) {
      return;
    }
    const remain = 9 - this.data.photos.length;
    if (remain <= 0) {
      wx.showToast({ title: '最多上传 9 张照片', icon: 'none' });
      return;
    }

    try {
      const result = await new Promise<any>((resolve, reject) => {
        wx.chooseMedia({
          count: remain,
          mediaType: ['image'],
          sizeType: ['compressed'],
          success: resolve,
          fail: reject,
        });
      });

      const files = result.tempFiles || [];
      if (!files.length) {
        return;
      }

      this.setData({ uploading: true });
      const uploaded: string[] = [];
      for (const file of files) {
        const url = await uploadImage(file.tempFilePath, 'community');
        uploaded.push(url);
      }
      const photoPreviews = await resolveImages(uploaded);
      this.setData({
        photos: this.data.photos.concat(uploaded),
        photoPreviews: this.data.photoPreviews.concat(photoPreviews),
      });
    } catch (error) {
      // 用户取消或上传失败
    } finally {
      this.setData({ uploading: false });
    }
  },

  removePhoto(event: any) {
    const index = Number(event.currentTarget.dataset.index);
    const photos = this.data.photos.slice();
    const photoPreviews = this.data.photoPreviews.slice();
    photos.splice(index, 1);
    photoPreviews.splice(index, 1);
    this.setData({ photos, photoPreviews });
  },

  async onPublish() {
    const { title, content, photos, currentPet, saving } = this.data;
    if (saving) {
      return;
    }
    if (!title.trim()) {
      wx.showToast({ title: '请填写标题', icon: 'none' });
      return;
    }
    if (!content.trim()) {
      wx.showToast({ title: '请填写正文', icon: 'none' });
      return;
    }

    this.setData({ saving: true });
    try {
      await createPost({
        title: title.trim(),
        content: content.trim(),
        petId: currentPet?.id,
        photoUrls: photos,
      });
      wx.showToast({ title: '发布成功', icon: 'success' });
      setTimeout(() => wx.navigateBack(), 500);
    } catch (error) {
      // request 已统一提示
    } finally {
      this.setData({ saving: false });
    }
  },
}, ["pet-placeholder.png"]));
