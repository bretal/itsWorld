import { requireLogin } from '../../../utils/auth';
import { applyAfterSale, getOrder } from '../../../services/commerce';
import type { OrderDetail } from '../../../services/commerce';
import { formatMoney } from '../../../utils/format';
import { uploadImage } from '../../../utils/upload';

const MAX_EVIDENCE = 3;

interface AfterSaleItemCard {
  id: number;
  title: string;
  skuId?: number;
  quantity: number;
  maxQty: number;
  qty: number;
  checked: boolean;
  priceText: string;
}

Page({
  data: {
    orderNo: '',
    loading: true,
    submitting: false,
    uploading: false,
    order: null as OrderDetail | null,
    items: [] as AfterSaleItemCard[],
    type: 'REFUND_ONLY' as 'REFUND_ONLY' | 'RETURN_REFUND',
    reason: '',
    description: '',
    evidenceUrls: [] as string[],
    maxEvidence: MAX_EVIDENCE,
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
      const order = await getOrder(this.data.orderNo);
      const shipped = ['SHIPPED', 'COMPLETED'].includes(order.orderStatus);
      const items = (order.items || [])
        .filter((item) => item.itemType === 'PRODUCT')
        .map((item) => ({
          id: item.id,
          title: item.titleSnapshot,
          skuId: item.skuId,
          quantity: item.quantity,
          maxQty: item.quantity,
          qty: item.quantity,
          checked: true,
          priceText: formatMoney(item.payableTotal),
        }));
      this.setData({
        order,
        items,
        type: shipped ? 'RETURN_REFUND' : 'REFUND_ONLY',
        loading: false,
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  onTypeChange(event: any) {
    this.setData({ type: event.currentTarget.dataset.type });
  },

  onToggleItem(event: any) {
    const id = Number(event.currentTarget.dataset.id);
    const items = this.data.items.map((item) =>
      item.id === id ? { ...item, checked: !item.checked } : item,
    );
    this.setData({ items });
  },

  onQtyChange(event: any) {
    const id = Number(event.currentTarget.dataset.id);
    const delta = Number(event.currentTarget.dataset.delta);
    const items = this.data.items.map((item) => {
      if (item.id !== id) return item;
      const qty = Math.min(Math.max(item.qty + delta, 1), item.maxQty);
      return { ...item, qty };
    });
    this.setData({ items });
  },

  onReasonInput(event: any) {
    this.setData({ reason: event.detail.value });
  },

  onDescriptionInput(event: any) {
    this.setData({ description: event.detail.value });
  },

  async chooseEvidence() {
    const remain = MAX_EVIDENCE - this.data.evidenceUrls.length;
    if (remain <= 0) {
      wx.showToast({ title: `最多上传 ${MAX_EVIDENCE} 张`, icon: 'none' });
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
          this.setData({
            evidenceUrls: this.data.evidenceUrls.concat(urls).slice(0, MAX_EVIDENCE),
          });
        } catch (error) {
          // uploadImage 已提示
        } finally {
          this.setData({ uploading: false });
        }
      },
    });
  },

  removeEvidence(event: any) {
    const index = Number(event.currentTarget.dataset.index);
    const evidenceUrls = this.data.evidenceUrls.filter((_, i) => i !== index);
    this.setData({ evidenceUrls });
  },

  previewEvidence(event: any) {
    const current = event.currentTarget.dataset.url;
    wx.previewImage({ current, urls: this.data.evidenceUrls });
  },

  async submit() {
    if (this.data.submitting) return;
    const selected = this.data.items.filter((item) => item.checked);
    if (!selected.length) {
      wx.showToast({ title: '请选择售后商品', icon: 'none' });
      return;
    }
    if (!this.data.reason.trim()) {
      wx.showToast({ title: '请填写申请原因', icon: 'none' });
      return;
    }
    try {
      this.setData({ submitting: true });
      const result = await applyAfterSale({
        orderNo: this.data.orderNo,
        type: this.data.type,
        reason: this.data.reason.trim(),
        description: this.data.description,
        evidenceUrls: this.data.evidenceUrls,
        items: selected.map((item) => ({ orderItemId: item.id, qty: item.qty })),
      });
      wx.showToast({ title: '申请已提交', icon: 'success' });
      wx.redirectTo({
        url: `/pages/commerce/after-sale/detail?afterSaleNo=${result.afterSaleNo}`,
      });
    } catch (error) {
      // request 已提示
    } finally {
      this.setData({ submitting: false });
    }
  },
});
