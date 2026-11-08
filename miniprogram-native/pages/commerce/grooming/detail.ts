import { withStaticImages } from '../../../utils/static-images';
import { requireLogin } from '../../../utils/auth';
import { getGroomingService } from '../../../services/commerce';
import type { GroomingServiceDetail } from '../../../services/commerce';
import { formatMoney } from '../../../utils/format';
import { resolveImage } from '../../../utils/image';

Page(withStaticImages({
  data: {
    id: 0,
    storeId: 0,
    loading: true,
    detail: null as GroomingServiceDetail | null,
    coverSrc: '',
    priceText: '0.00',
    memberPriceText: '',
    petText: '',
    ruleTexts: [] as string[],
  },

  onLoad(options: any) {
    if (!requireLogin()) {
      return;
    }
    const id = Number(options.id || 0);
    const storeId = Number(options.storeId || 0);
    let petId = Number(options.petId || 0);
    if (!petId) {
      petId = Number(wx.getStorageSync('currentPetId') || 0);
    }
    this.setData({ id, storeId });
    this.loadDetail(id, storeId, petId);
  },

  async loadDetail(id: number, storeId: number, petId: number) {
    try {
      const detail = await getGroomingService(id, storeId || undefined, petId || undefined);
      const price = detail.storePrice ?? detail.basePrice;
      const memberPrice = detail.memberPrice ?? price;
      this.setData({
        detail,
        coverSrc: await resolveImage(detail.coverUrl),
        priceText: formatMoney(price),
        memberPriceText:
          memberPrice < price ? `会员价 ¥${formatMoney(memberPrice)}` : '',
        petText:
          detail.petEligible === undefined
            ? ''
            : detail.petEligible
              ? '当前宠物符合服务条件'
              : detail.ineligibleReason || '当前宠物不符合服务条件',
        ruleTexts: (detail.petRules || []).map((rule) => {
          const weight =
            rule.minWeightGram || rule.maxWeightGram
              ? `${rule.minWeightGram ?? 0}-${rule.maxWeightGram ?? '不限'}g`
              : '';
          const age =
            rule.minAgeMonth || rule.maxAgeMonth
              ? `${rule.minAgeMonth ?? 0}-${rule.maxAgeMonth ?? '不限'}月龄`
              : '';
          return [rule.speciesCode, weight, age, rule.note].filter(Boolean).join(' · ');
        }),
        loading: false,
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  goBooking() {
    if (this.data.detail?.petEligible === false) {
      wx.showToast({
        title: this.data.detail.ineligibleReason || '当前宠物不符合服务条件',
        icon: 'none',
      });
      return;
    }
    const petId = Number(wx.getStorageSync('currentPetId') || 0);
    wx.navigateTo({
      url: `/pages/commerce/booking/index?serviceId=${this.data.id}&storeId=${this.data.storeId}&petId=${petId}`,
    });
  },
}, ["pet-placeholder.png"]));
