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
}

Page(withStaticImages({
  data: {
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
          priceText: formatMoney(item.memberPrice),
        };
      });
      this.setData({ items, loading: false });
      this.recalculate();
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  recalculate() {
    const validSelected = this.data.items.filter(
      (item) => item.selected === 1 && !item.invalidReason,
    );
    const total = validSelected.reduce(
      (sum, item) => sum + item.memberPrice * item.quantity,
      0,
    );
    this.setData({
      totalText: formatMoney(total),
      selectedCount: validSelected.reduce((sum, item) => sum + item.quantity, 0),
      allSelected:
        this.data.items.length > 0 &&
        this.data.items.every((item) => item.selected === 1),
    });
  },

  async changeQty(event: any) {
    const id = Number(event.currentTarget.dataset.id);
    const delta = Number(event.currentTarget.dataset.delta);
    const item = this.data.items.find((row) => row.id === id);
    if (!item) return;
    const quantity = item.quantity + delta;
    if (quantity < 1) return;
    try {
      await updateCartItem(id, { quantity });
      await this.loadCart();
    } catch (error) {
      // request 已提示
    }
  },

  async toggleSelect(event: any) {
    const id = Number(event.currentTarget.dataset.id);
    const item = this.data.items.find((row) => row.id === id);
    if (!item) return;
    try {
      await updateCartItem(id, { selected: item.selected === 1 ? 0 : 1 });
      await this.loadCart();
    } catch (error) {
      // request 已提示
    }
  },

  async toggleAll() {
    const next = this.data.allSelected ? 0 : 1;
    try {
      await Promise.all(
        this.data.items.map((item) =>
          updateCartItem(item.id, { selected: next }),
        ),
      );
      await this.loadCart();
    } catch (error) {
      // request 已提示
    }
  },

  removeItem(event: any) {
    const id = Number(event.currentTarget.dataset.id);
    wx.showModal({
      title: '移除商品',
      content: '确定从购物车移除该商品吗？',
      success: async (result) => {
        if (!result.confirm) return;
        try {
          await removeCartItem(id);
          await this.loadCart();
        } catch (error) {
          // request 已提示
        }
      },
    });
  },

  goCheckout() {
    const hasSelected = this.data.items.some(
      (item) => item.selected === 1 && !item.invalidReason,
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
