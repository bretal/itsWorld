import { withStaticImages } from '../../../utils/static-images';
import { requireLogin } from '../../../utils/auth';
import {
  listGroomingCategories,
  listGroomingServices,
  listStores,
} from '../../../services/commerce';
import type {
  CommerceStore,
  GroomingCategory,
  GroomingServiceItem,
} from '../../../services/commerce';
import { formatMoney } from '../../../utils/format';
import { resolveImage } from '../../../utils/image';

interface ServiceCard extends GroomingServiceItem {
  coverSrc: string;
  priceText: string;
  memberPriceText: string;
  durationText: string;
}

Page(withStaticImages({
  data: {
    stores: [] as CommerceStore[],
    storeNames: [] as string[],
    storeIndex: 0,
    categories: [] as GroomingCategory[],
    activeCategoryId: 0,
    services: [] as ServiceCard[],
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
    this.loadStores();
  },

  onPullDownRefresh() {
    this.loadServices(true).finally(() => wx.stopPullDownRefresh());
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading && !this.data.loadingMore) {
      this.loadServices(false);
    }
  },

  async loadStores() {
    try {
      const result = await listStores(1, 100);
      const stores = result.records || [];
      this.setData({
        stores,
        storeNames: stores.map((store) => store.name),
        storeIndex: 0,
      });
      await this.loadServices(true);
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  async loadCategories() {
    try {
      const categories = await listGroomingCategories();
      this.setData({ categories });
    } catch (error) {
      // 分类失败不阻塞服务列表
    }
  },

  async loadServices(reset: boolean) {
    const page = reset ? 1 : this.data.page + 1;
    this.setData(reset ? { loading: true } : { loadingMore: true });
    try {
      const store = this.data.stores[this.data.storeIndex];
      const categoryId = this.data.activeCategoryId || undefined;
      const result = await listGroomingServices(
        store?.id,
        categoryId,
        page,
        this.data.size,
      );
      const services = await Promise.all(
        (result.records || []).map(async (service) => {
          const price = service.storePrice ?? service.basePrice;
          const memberPrice = service.memberPrice ?? price;
          return {
            ...service,
            coverSrc: await resolveImage(service.coverUrl),
            priceText: formatMoney(price),
            memberPriceText:
              memberPrice < price ? `会员价 ¥${formatMoney(memberPrice)}` : '',
            durationText: `${service.durationMin || 0} 分钟`,
          };
        }),
      );
      this.setData({
        services: reset ? services : this.data.services.concat(services),
        page: result.pageNum,
        hasMore: result.pageNum < result.totalPages,
        loading: false,
        loadingMore: false,
      });
    } catch (error) {
      this.setData({ loading: false, loadingMore: false });
    }
  },

  onStoreChange(event: any) {
    const storeIndex = Number(event.detail.value);
    this.setData({ storeIndex });
    this.loadServices(true);
  },

  onTapCategory(event: any) {
    const id = Number(event.currentTarget.dataset.id) || 0;
    this.setData({ activeCategoryId: id });
    this.loadServices(true);
  },

  goServiceDetail(event: any) {
    const id = Number(event.currentTarget.dataset.id);
    const store = this.data.stores[this.data.storeIndex];
    const storeId = store ? `&storeId=${store.id}` : '';
    wx.navigateTo({ url: `/pages/commerce/grooming/detail?id=${id}${storeId}` });
  },

  goStores() {
    wx.navigateTo({ url: '/pages/commerce/store/index' });
  },
}, ["pet-placeholder.png"]));
