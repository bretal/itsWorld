import { withStaticImages } from '../../../utils/static-images';
import { requireLogin } from '../../../utils/auth';
import {
  listAddresses,
  previewCheckout,
  submitCheckout,
} from '../../../services/commerce';
import type {
  CheckoutItem,
  CheckoutPreview,
  CouponOption,
  ShippingAddress,
} from '../../../services/commerce';
import { formatMoney } from '../../../utils/format';
import { resolveImages } from '../../../utils/image';

interface CheckoutItemView extends CheckoutItem {
  coverSrc: string;
  specText: string;
  priceText: string;
  lineTotalText: string;
}

Page(withStaticImages({
  data: {
    loading: true,
    submitting: false,
    addresses: [] as ShippingAddress[],
    addressNames: [] as string[],
    addressIndex: 0,
    preview: null as CheckoutPreview | null,
    items: [] as CheckoutItemView[],
    couponOptions: [] as CouponOption[],
    couponLabels: [] as string[],
    couponIndex: 0,
    originalText: '0.00',
    memberDiscountText: '0.00',
    couponDiscountText: '0.00',
    payableText: '0.00',
    walletText: '0.00',
  },

  onShow() {
    if (!requireLogin()) {
      return;
    }
    this.loadAll();
  },

  onPullDownRefresh() {
    this.loadAll().finally(() => wx.stopPullDownRefresh());
  },

  async loadAll() {
    try {
      await this.loadAddresses();
      await this.loadPreview();
      this.setData({ loading: false });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  async loadAddresses() {
    try {
      const addresses = await listAddresses();
      let addressIndex = addresses.findIndex((address) => address.isDefault === 1);
      if (addressIndex < 0) {
        addressIndex = addresses.length ? 0 : -1;
      }
      this.setData({
        addresses,
        addressNames: addresses.map((address) =>
          `${address.recipientName} ${address.mobile} ${address.province || ''}${address.city || ''}${address.district || ''}${address.addressDetail}`,
        ),
        addressIndex,
      });
    } catch (error) {
      // request 已提示
    }
  },

  async loadPreview() {
    const couponOptions = this.data.couponOptions;
    const currentCoupon = this.data.couponIndex > 0
      ? couponOptions[this.data.couponIndex - 1]
      : undefined;
    const preview = await previewCheckout(currentCoupon?.id);
    const images = await resolveImages(
      preview.items.map((item) => item.productCoverUrl),
    );
    const items = preview.items.map((item, index) => {
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
        priceText: formatMoney(item.memberUnitPrice),
        lineTotalText: formatMoney(item.payableTotal),
      };
    });
    const options = preview.couponOptions || [];
    let couponIndex = this.data.couponIndex;
    if (couponIndex > options.length) {
      couponIndex = 0;
    }
    this.setData({
      preview,
      items,
      couponOptions: options,
      couponLabels: [
        '不使用优惠券',
        ...options.map(
          (coupon) =>
            `${coupon.templateName} -¥${formatMoney(coupon.discountAmount)}`,
        ),
      ],
      couponIndex,
      originalText: formatMoney(preview.originalAmount),
      memberDiscountText: formatMoney(preview.memberDiscount),
      couponDiscountText: formatMoney(preview.couponDiscount),
      payableText: formatMoney(preview.payableAmount),
      walletText: formatMoney(preview.walletAvailableBalance),
    });
  },

  onAddressChange(event: any) {
    this.setData({ addressIndex: Number(event.detail.value) });
  },

  async onCouponChange(event: any) {
    const couponIndex = Number(event.detail.value);
    this.setData({ couponIndex });
    try {
      await this.loadPreview();
    } catch (error) {
      // request 已提示
    }
  },

  goAddressList() {
    wx.navigateTo({ url: '/pages/commerce/address/index?from=checkout' });
  },

  goAddAddress() {
    wx.navigateTo({ url: '/pages/commerce/address/edit' });
  },

  async submitOrder() {
    if (this.data.submitting) {
      return;
    }
    const address = this.data.addresses[this.data.addressIndex];
    if (!address) {
      wx.showToast({ title: '请先添加收货地址', icon: 'none' });
      return;
    }
    if (!this.data.preview?.valid) {
      wx.showToast({ title: '存在失效商品，请返回购物车处理', icon: 'none' });
      return;
    }
    const coupon =
      this.data.couponIndex > 0
        ? this.data.couponOptions[this.data.couponIndex - 1]
        : undefined;
    try {
      this.setData({ submitting: true });
      const result = await submitCheckout({
        idempotencyKey: `MP-${Date.now()}-${Math.floor(Math.random() * 10000)}`,
        couponId: coupon?.id,
        addressId: address.id,
      });
      wx.redirectTo({
        url: `/pages/commerce/order/detail?orderNo=${result.orderNo}`,
      });
    } catch (error) {
      // request 已提示
    } finally {
      this.setData({ submitting: false });
    }
  },
}, ["pet-placeholder.png"]));
