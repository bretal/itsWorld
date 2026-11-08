import { withStaticImages } from '../../../utils/static-images';
import { requireLogin } from '../../../utils/auth';
import { listPackages } from '../../../services/commerce';
import type { PackageTemplate } from '../../../services/commerce';
import { formatMoney } from '../../../utils/format';
import { resolveImage } from '../../../utils/image';

interface PackageCard extends PackageTemplate {
  coverSrc: string;
  priceText: string;
  validText: string;
  itemTexts: string[];
}

Page(withStaticImages({
  data: {
    loading: true,
    loadingMore: false,
    page: 1,
    size: 10,
    hasMore: true,
    keyword: '',
    list: [] as PackageCard[],
  },

  onShow() {
    if (!requireLogin()) {
      return;
    }
    this.load(true);
  },

  onPullDownRefresh() {
    this.load(true).finally(() => wx.stopPullDownRefresh());
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading && !this.data.loadingMore) {
      this.load(false);
    }
  },

  async load(reset: boolean) {
    const page = reset ? 1 : this.data.page + 1;
    this.setData(reset ? { loading: true } : { loadingMore: true });
    try {
      const result = await listPackages(
        this.data.keyword || undefined,
        page,
        this.data.size,
      );
      const list = await Promise.all(
        (result.records || []).map(async (template) => ({
          ...template,
          coverSrc: await resolveImage(template.coverUrl),
          priceText: formatMoney(template.salePrice),
          validText: template.validDays ? `${template.validDays} 天有效` : '长期有效',
          itemTexts: [
            ...(template.items || []).map(
              (item) => `${item.serviceName || '服务'} ×${item.includedTimes}`,
            ),
            ...(template.options || []).map(
              (option) =>
                `${option.optionName}（选 ${option.minSelect}-${option.maxSelect} 项）`,
            ),
          ],
        })),
      );
      this.setData({
        list: reset ? list : this.data.list.concat(list),
        page: result.pageNum,
        hasMore: result.pageNum < result.totalPages,
        loading: false,
        loadingMore: false,
      });
    } catch (error) {
      this.setData({ loading: false, loadingMore: false });
    }
  },

  onSearch(event: any) {
    this.setData({ keyword: event.detail.value });
    this.load(true);
  },

  goDetail(event: any) {
    const id = Number(event.currentTarget.dataset.id);
    wx.navigateTo({ url: `/pages/commerce/package/detail?id=${id}` });
  },

  goMine() {
    wx.navigateTo({ url: '/pages/commerce/package/mine' });
  },
}, ["pet-placeholder.png"]));
