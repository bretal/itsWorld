import { withStaticImages } from '../../../utils/static-images';
import { requireLogin } from '../../../utils/auth';
import { listShopCategories, listShopProducts } from '../../../services/commerce';
import type { ShopCategory, ShopProductItem } from '../../../services/commerce';
import { formatMoney } from '../../../utils/format';
import { resolveImage } from '../../../utils/image';

interface ProductCard extends ShopProductItem {
  coverSrc: string;
  priceText: string;
  memberPriceText: string;
}

Page(withStaticImages({
  data: {
    categories: [] as ShopCategory[],
    activeCategoryId: 0,
    products: [] as ProductCard[],
    loading: true,
    loadingMore: false,
    page: 1,
    size: 10,
    hasMore: true,
  },

  onLoad() {
    if (!requireLogin()) {
      return;
    }
    this.loadCategories();
    this.loadProducts(true);
  },

  onPullDownRefresh() {
    this.loadProducts(true).finally(() => wx.stopPullDownRefresh());
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading && !this.data.loadingMore) {
      this.loadProducts(false);
    }
  },

  async loadCategories() {
    try {
      const categories = await listShopCategories();
      this.setData({ categories });
    } catch (error) {
      // 分类失败不阻塞商品列表
    }
  },

  async loadProducts(reset: boolean) {
    const page = reset ? 1 : this.data.page + 1;
    this.setData(reset ? { loading: true } : { loadingMore: true });
    try {
      const categoryId = this.data.activeCategoryId || undefined;
      const result = await listShopProducts(categoryId, undefined, page, this.data.size);
      const products = await Promise.all(
        (result.records || []).map(async (product) => {
          const price = product.minPrice ?? 0;
          const memberPrice = product.minMemberPrice ?? price;
          return {
            ...product,
            coverSrc: await resolveImage(product.coverUrl),
            priceText: formatMoney(price),
            memberPriceText:
              memberPrice < price ? `会员价 ¥${formatMoney(memberPrice)}` : '',
          };
        }),
      );
      this.setData({
        products: reset ? products : this.data.products.concat(products),
        page: result.pageNum,
        hasMore: result.pageNum < result.totalPages,
        loading: false,
        loadingMore: false,
      });
    } catch (error) {
      this.setData({ loading: false, loadingMore: false });
    }
  },

  onTapCategory(event: any) {
    const id = Number(event.currentTarget.dataset.id) || 0;
    this.setData({ activeCategoryId: id });
    this.loadProducts(true);
  },

  goProductDetail(event: any) {
    const id = Number(event.currentTarget.dataset.id);
    wx.navigateTo({ url: `/pages/commerce/shop/detail?id=${id}` });
  },
}, ["pet-placeholder.png"]));
