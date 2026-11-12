import { addCartItem, getShopProduct } from '../../services/commerce';
import type { ShopProductDetail, ShopSkuItem } from '../../services/commerce';
import { formatMoney } from '../../utils/format';
import { resolveImages } from '../../utils/image';

interface SkuCard extends ShopSkuItem {
  priceText: string;
  memberPriceText: string;
  specText: string;
}

Component({
  properties: {
    productId: { type: String, value: '', observer(id: string) { if (id) this.loadDetail(id); } },
    inSheet: { type: Boolean, value: false },
  },
  data: {
    asset: '/assets/shop/',
    quantity: 1,
    submitting: false,
    imageIndex: 1,
    loadError: false,
    loading: true,
    detail: null as ShopProductDetail | null,
    imageUrls: [] as string[],
    skus: [] as SkuCard[],
    selectedSkuId: 0,
    priceText: '0.00',
    memberPriceText: '',
    stockText: '0',
    selectedSpecText: '',
  },

  lifetimes: {
    detached() { this._loadSequence++; },
  },

  methods: {
    retry() { this.loadDetail(this.data.productId); },

    async loadDetail(id: string) {
      const sequence = this._loadSequence = (this._loadSequence || 0) + 1;
      this.setData({ loading: true, loadError: false, detail: null, quantity: 1, imageIndex: 1,
        selectedSkuId: 0, priceText: '0.00', memberPriceText: '', stockText: '0', selectedSpecText: '' });
      try {
        const detail = await getShopProduct(id);
        const gallery = (detail.images || []).map(image => image.imageUrl)
          .filter(url => typeof url === 'string' && url.trim().length > 0);
        const sources = gallery.length ? gallery : (detail.coverUrl?.trim() ? [detail.coverUrl] : []);
        const imageUrls = await resolveImages(sources);

        if (sequence !== this._loadSequence) return;
        const skus: SkuCard[] = (detail.skus || []).map((sku) => {
          let specText = '';
          if (sku.specJson) {
            try {
              const parsed = JSON.parse(sku.specJson);
              specText = Object.values(parsed).join(' / ');
            } catch (error) {
              specText = sku.specJson;
            }
          }
          const price = sku.salePrice;
          const memberPrice = sku.memberPrice ?? price;
          return {
            ...sku,
            priceText: formatMoney(price),
            memberPriceText:
              memberPrice < price ? `会员 ¥${formatMoney(memberPrice)}` : '',
            specText: specText || sku.skuCode,
          };
        });
        const firstSku = skus.find((sku) => sku.status === 'ENABLED' && sku.availableQty > 0);
        this.setData({
          detail,
          imageUrls,
          skus,
          selectedSkuId: firstSku?.id || 0,
          ...(firstSku
            ? {
                priceText: firstSku.priceText,
                memberPriceText: firstSku.memberPriceText,
                stockText: String(firstSku.availableQty),
                selectedSpecText: firstSku.specText,
              }
            : {}),
          loading: false,
        });
      } catch (_) {
        if (sequence === this._loadSequence) this.setData({ loading: false, loadError: true });
      }
    },

    selectSku(event: any) {
      const id = Number(event.currentTarget.dataset.id);
      const sku = this.data.skus.find((item) => item.id === id);
      if (!sku || sku.status !== 'ENABLED' || sku.availableQty < 1) {
        return;
      }
      this.setData({
        quantity: 1,
        selectedSkuId: id,
        priceText: sku.priceText,
        memberPriceText: sku.memberPriceText,
        stockText: String(sku.availableQty),
        selectedSpecText: sku.specText,
      });
    },

    changeQuantity(event: any) {
      const quantity = this.data.quantity + Number(event.currentTarget.dataset.delta);
      if (quantity >= 1 && quantity <= Number(this.data.stockText)) this.setData({ quantity });
    },
    onImageError(event: any) {
      const imageUrls = this.data.imageUrls.filter((url: string) => url !== event.currentTarget.dataset.src);
      this.setData({ imageUrls, imageIndex: Math.max(1, Math.min(this.data.imageIndex, imageUrls.length)) });
    },
    onImageChange(event: any) { this.setData({ imageIndex: event.detail.current + 1 }); },
    previewImage(event: any) {
      wx.previewImage({ current: event.currentTarget.dataset.src, urls: this.data.imageUrls });
    },
    async addToCart(event?: any) {
      if (this.data.submitting) return;
      if (!this.data.selectedSkuId || Number(this.data.stockText) < this.data.quantity) {
        wx.showToast({ title: '请选择有库存的规格', icon: 'none' });
        return;
      }
      this.setData({ submitting: true });
      try {
        await addCartItem(this.data.selectedSkuId, this.data.quantity);
        if (event?.currentTarget?.dataset.checkout) this.goCart();
        else wx.showToast({ title: '已加入购物车', icon: 'success' });
      } catch (_) { /* 请求层统一提示。 */ }
      finally { this.setData({ submitting: false }); }
    },
    goShop() { wx.navigateTo({ url: '/pages/commerce/shop/index' }); },
    goMember() { wx.navigateTo({ url: '/pages/member/index' }); },
    goCart() {
      wx.navigateTo({ url: '/pages/commerce/cart/index' });
    },
  },
});
