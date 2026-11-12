import { withStaticImages } from '../../../utils/static-images';
import { requireLogin } from '../../../utils/auth';
import {
  getCart,
  updateCartItem,
  removeCartItem,
} from '../../../services/commerce';
import type { CartItem } from '../../../services/commerce';
import { formatMoney } from '../../../utils/format';
import { resolveImages } from '../../../utils/image';

interface CartView extends CartItem {
  coverSrc: string;
  specText: string;
  priceText: string;
  originalText: string;
  hasDiscount: boolean;
}

Page(withStaticImages({
  data: {
    asset: '/assets/shop/',
    busy: false,
    loadError: false,
    savingsText: '0.00',
    items: [] as CartView[],
    loading: true,
    totalText: '0.00',
    selectedCount: 0,
    allSelected: false,
  },

  onShow() {
    if (!requireLogin()) {
      return;
    }
    this.loadCart();
  },

  onPullDownRefresh() {
    this.loadCart().finally(() => wx.stopPullDownRefresh());
  },

  async loadCart() {
    try {
      const cart = await getCart();
      const images = await resolveImages(cart.map((item) => item.productCoverUrl));
      const items = cart.map((item, index) => {
        let specText = '';
        if (item.specJson) {
          try {
            specText = Object.values(JSON.parse(item.specJson)).join(' / ');
          } catch (error) {
            specText = item.specJson;
          }
        }
        return {
          ...item,
          coverSrc: images[index] || '',
          specText,
          priceText: formatMoney(item.memberPrice ?? item.salePrice),
          originalText: formatMoney(item.salePrice),
          hasDiscount: item.memberPrice != null && item.memberPrice < item.salePrice,
        };
      });
      this.setData({ items, loading: false, loadError: false });
      this.recalculate();
    } catch (error) {
      this.setData({ loading: false, loadError: true });
    }
  },

  recalculate() {
    const validSelected = this.data.items.filter(
      (item) => item.selected === 1 && !item.invalidReason && item.availableQty >= item.quantity,
    );
    const total = validSelected.reduce(
      (sum, item) => sum + (item.memberPrice ?? item.salePrice) * item.quantity,
      0,
    );
    this.setData({
      totalText: formatMoney(total),
      savingsText: formatMoney(validSelected.reduce((sum, item) => sum + Math.max(0, item.salePrice - (item.memberPrice ?? item.salePrice)) * item.quantity, 0)),
      selectedCount: validSelected.reduce((sum, item) => sum + item.quantity, 0),
      allSelected:
        this.data.items.some(item => !item.invalidReason && item.availableQty >= item.quantity) &&
        this.data.items.filter(item => !item.invalidReason && item.availableQty >= item.quantity).every(item => item.selected === 1),
    });
  },

  async changeQty(event: any) {
    const id = Number(event.currentTarget.dataset.id);
    const delta = Number(event.currentTarget.dataset.delta);
    const item = this.data.items.find((row) => row.id === id);
    if (!item) return;
    const quantity = item.quantity + delta;
    if (this.data.busy || quantity < 1 || quantity > item.availableQty || item.invalidReason) return;
    this.setData({ busy: true });
    try {
      await updateCartItem(id, { quantity });
      await this.loadCart();
    } catch (error) {
      await this.loadCart();
    } finally { this.setData({ busy: false }); }
  },

  async toggleSelect(event: any) {
    const id = Number(event.currentTarget.dataset.id);
    const item = this.data.items.find((row) => row.id === id);
    if (!item || this.data.busy || item.invalidReason || item.availableQty < item.quantity) return;
    this.setData({ busy: true });
    try {
      await updateCartItem(id, { selected: item.selected === 1 ? 0 : 1 });
      await this.loadCart();
    } catch (error) {
      await this.loadCart();
    } finally { this.setData({ busy: false }); }
  },

  async toggleAll() {
    if (this.data.busy) return;
    this.setData({ busy: true });
    const next = this.data.allSelected ? 0 : 1;
    try {
      await Promise.all(
        this.data.items.filter(item => !item.invalidReason && item.availableQty >= item.quantity).map((item) =>
          updateCartItem(item.id, { selected: next }),
        ),
      );
      await this.loadCart();
    } catch (error) {
      await this.loadCart();
    } finally { this.setData({ busy: false }); }
  },

  imageError(event: any) {
    const id = Number(event.currentTarget.dataset.id);
    this.setData({ items: this.data.items.map(item => item.id === id ? { ...item, coverSrc: '' } : item) });
  },
  clearCart() {
    if (this.data.busy) return;
    wx.showModal({ title: '清空购物车', content: '确定移除购物车中的全部商品吗？', success: async result => {
      if (!result.confirm || this.data.busy) return;
      this.setData({ busy: true });
      try { await Promise.all(this.data.items.map(item => removeCartItem(item.id))); }
      catch (_) { /* 请求层提示，重新查询实际状态。 */ }
      finally { await this.loadCart(); this.setData({ busy: false }); }
    } });
  },
  goMember() { wx.navigateTo({ url: '/pages/member/index' }); },
  removeItem(event: any) {
    if (this.data.busy) return;
    const id = Number(event.currentTarget.dataset.id);
    wx.showModal({
      title: '移除商品',
      content: '确定从购物车移除该商品吗？',
      success: async (result) => {
        if (!result.confirm || this.data.busy) return;
        this.setData({ busy: true });
        try {
          await removeCartItem(id);
          await this.loadCart();
        } catch (error) {
          await this.loadCart();
        } finally { this.setData({ busy: false }); }
      },
    });
  },

  goCheckout() {
    if (this.data.busy) return;
    const hasSelected = this.data.items.some(
      (item) => item.selected === 1 && !item.invalidReason && item.availableQty >= item.quantity,
    );
    if (!hasSelected) {
      wx.showToast({ title: '请选择有效商品', icon: 'none' });
      return;
    }
    wx.navigateTo({ url: '/pages/commerce/checkout/index' });
  },

  goShop() {
    wx.navigateTo({ url: '/pages/commerce/shop/index' });
  },
}, ["pet-placeholder.png"]));
