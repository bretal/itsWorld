import { withStaticImages } from '../../../utils/static-images';
import { requireLogin } from '../../../utils/auth';
import { addCartItem, getShopProduct } from '../../../services/commerce';
import type { ShopProductDetail, ShopSkuItem } from '../../../services/commerce';
import { formatMoney } from '../../../utils/format';
import { resolveImages } from '../../../utils/image';

interface SkuCard extends ShopSkuItem {
  priceText: string;
  memberPriceText: string;
  specText: string;
}

Page(withStaticImages({
  data: {
    id: 0,
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

  onLoad(options: any) {
    if (!requireLogin()) {
      return;
    }
    const id = Number(options.id || 0);
    this.setData({ id });
    this.loadDetail(id);
  },

  async loadDetail(id: number) {
    try {
      const detail = await getShopProduct(id);
      const imageUrls = await resolveImages(
        (detail.images || []).map((image) => image.imageUrl),
      );
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
      const firstSku = skus.find((sku) => sku.status === 'ENABLED') || skus[0];
      this.setData({
        detail,
        imageUrls: imageUrls.length
          ? imageUrls
          : [detail.coverUrl || ''],
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
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  selectSku(event: any) {
    const id = Number(event.currentTarget.dataset.id);
    const sku = this.data.skus.find((item) => item.id === id);
    if (!sku || sku.status !== 'ENABLED') {
      return;
    }
    this.setData({
      selectedSkuId: id,
      priceText: sku.priceText,
      memberPriceText: sku.memberPriceText,
      stockText: String(sku.availableQty),
      selectedSpecText: sku.specText,
    });
  },

  async addToCart() {
    if (!this.data.selectedSkuId) {
      wx.showToast({ title: '请选择规格', icon: 'none' });
      return;
    }
    try {
      await addCartItem(this.data.selectedSkuId, 1);
      wx.showToast({ title: '已加入购物车', icon: 'success' });
    } catch (error) {
      // request 已提示
    }
  },

  goCart() {
    wx.navigateTo({ url: '/pages/commerce/cart/index' });
  },
}, ["pet-placeholder.png"]));
