import { requireLogin } from '../../../utils/auth';
import { getOrderReview, submitReview } from '../../../services/commerce';
import type { ReviewView } from '../../../services/commerce';
import { uploadImage } from '../../../utils/upload';

const MAX_IMAGES = 3;

Page({
  data: {
    orderNo: '',
    loading: true,
    submitting: false,
    uploading: false,
    stars: [1, 2, 3, 4, 5],
    rating: 5,
    content: '',
    images: [] as string[],
    maxImages: MAX_IMAGES,
    existing: null as ReviewView | null,
  },

  onLoad(options: any) {
    if (!requireLogin()) {
      return;
    }
    this.setData({ orderNo: options.orderNo || '' });
    this.load();
  },

  async load() {
    try {
      const existing = await getOrderReview(this.data.orderNo);
      this.setData({ existing, loading: false });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  onSelectRating(event: any) {
    this.setData({ rating: Number(event.currentTarget.dataset.value) });
  },

  onContentInput(event: any) {
    this.setData({ content: event.detail.value });
  },

  async chooseImages() {
    const remain = MAX_IMAGES - this.data.images.length;
    if (remain <= 0) {
      wx.showToast({ title: `最多上传 ${MAX_IMAGES} 张`, icon: 'none' });
      return;
    }
    wx.chooseMedia({
      count: remain,
      mediaType: ['image'],
      success: async (result: any) => {
        try {
          this.setData({ uploading: true });
          const urls: string[] = [];
          for (const file of result.tempFiles) {
            urls.push(await uploadImage(file.tempFilePath, 'review'));
          }
          this.setData({ images: this.data.images.concat(urls).slice(0, MAX_IMAGES) });
        } catch (error) {
          // uploadImage 已提示
        } finally {
          this.setData({ uploading: false });
        }
      },
    });
  },

  removeImage(event: any) {
    const index = Number(event.currentTarget.dataset.index);
    this.setData({ images: this.data.images.filter((_, i) => i !== index) });
  },

  previewImage(event: any) {
    const current = event.currentTarget.dataset.url;
    wx.previewImage({ current, urls: this.data.images });
  },

  async submit() {
    if (this.data.submitting) return;
    if (!this.data.rating) {
      wx.showToast({ title: '请选择评分', icon: 'none' });
      return;
    }
    try {
      this.setData({ submitting: true });
      await submitReview({
        orderNo: this.data.orderNo,
        rating: this.data.rating,
        content: this.data.content,
        images: this.data.images,
      });
      wx.showToast({ title: '评价已提交', icon: 'success' });
      setTimeout(() => {
        wx.navigateBack();
      }, 800);
    } catch (error) {
      // request 已提示
    } finally {
      this.setData({ submitting: false });
    }
  },
});
