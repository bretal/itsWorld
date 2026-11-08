import { withStaticImages } from '../../../utils/static-images';
import { requireLogin } from '../../../utils/auth';
import { listPets } from '../../../services/pet';
import {
  getPackage,
  listStores,
  previewPackage,
  purchasePackage,
} from '../../../services/commerce';
import type {
  CommerceStore,
  PackageOptionView,
  PackagePurchasePreview,
  PackageTemplate,
} from '../../../services/commerce';
import type { Pet } from '../../../services/pet';
import { formatMoney } from '../../../utils/format';
import { resolveImage } from '../../../utils/image';

interface OptionItemCard {
  id: number;
  serviceId: number;
  serviceName?: string;
  addPrice: number;
  addPriceText: string;
  includedTimes: number;
  selected: boolean;
}

interface OptionCard extends Omit<PackageOptionView, 'items'> {
  items: OptionItemCard[];
  selectedCount: number;
  hintText: string;
}

Page(withStaticImages({
  data: {
    id: 0,
    loading: true,
    submitting: false,
    template: null as PackageTemplate | null,
    coverSrc: '',
    priceText: '0.00',
    validText: '',
    fixedItemTexts: [] as string[],
    options: [] as OptionCard[],
    selectedOptionItemIds: [] as number[],
    pets: [] as Pet[],
    petNames: [] as string[],
    petIndex: 0,
    stores: [] as CommerceStore[],
    storeNames: [] as string[],
    storeIndex: 0,
    preview: null as PackagePurchasePreview | null,
    previewItems: [] as any[],
    couponOptions: [] as any[],
    couponLabels: [] as string[],
    couponIndex: 0,
    originalText: '0.00',
    memberDiscountText: '0.00',
    couponDiscountText: '0.00',
    payableText: '0.00',
    walletText: '0.00',
  },

  onLoad(options: any) {
    if (!requireLogin()) {
      return;
    }
    const id = Number(options.id || 0);
    this.setData({ id });
    this.loadAll();
  },

  onPullDownRefresh() {
    this.loadAll().finally(() => wx.stopPullDownRefresh());
  },

  async loadAll() {
    try {
      await Promise.all([this.loadTemplate(), this.loadPets(), this.loadStores()]);
      await this.refreshPreview();
      this.setData({ loading: false });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  async loadTemplate() {
    const template = await getPackage(this.data.id);
    this.setData({
      template,
      coverSrc: await resolveImage(template.coverUrl),
      priceText: formatMoney(template.salePrice),
      validText: template.validDays ? `${template.validDays} 天有效` : '长期有效',
      fixedItemTexts: (template.items || []).map(
        (item) => `${item.serviceName || '服务'} ×${item.includedTimes}`,
      ),
      options: (template.options || []).map((option) =>
        this.toOptionCard(
          {
            ...option,
            items: (option.items || []).filter((item) => item.status === 'ENABLED'),
          },
          [],
        ),
      ),
    });
  },

  async loadPets() {
    const pets = await listPets();
    let petIndex = 0;
    const storedPetId = Number(wx.getStorageSync('currentPetId') || 0);
    if (storedPetId) {
      const found = pets.findIndex((pet) => pet.id === storedPetId);
      if (found >= 0) {
        petIndex = found;
      }
    }
    this.setData({
      pets,
      petNames: pets.map((pet) => pet.petName),
      petIndex,
    });
  },

  async loadStores() {
    const result = await listStores(1, 100);
    const stores = result.records || [];
    this.setData({
      stores,
      storeNames: stores.map((store) => store.name),
      storeIndex: 0,
    });
  },

  toOptionCard(option: PackageOptionView, selectedIds: number[]): OptionCard {
    const items = (option.items || []).map((item) => ({
      id: item.id,
      serviceId: item.serviceId,
      serviceName: item.serviceName,
      addPrice: item.addPrice || 0,
      addPriceText: formatMoney(item.addPrice || 0),
      includedTimes: item.includedTimes,
      selected: selectedIds.includes(item.id),
    }));
    const selectedCount = items.filter((item) => item.selected).length;
    return {
      ...option,
      items,
      selectedCount,
      hintText:
        option.maxSelect === 1
          ? `必选 ${option.required === 1 ? '1' : '0'} 项，选 1 项`
          : `选 ${option.minSelect}-${option.maxSelect} 项`,
    };
  },

  onToggleOption(event: any) {
    const optionId = Number(event.currentTarget.dataset.optionId);
    const itemId = Number(event.currentTarget.dataset.itemId);
    const options = this.data.options.map((option) => {
      if (option.id !== optionId) {
        return option;
      }
      let items = option.items.map((item) =>
        item.id === itemId ? { ...item, selected: !item.selected } : item,
      );
      if (option.maxSelect === 1) {
        items = items.map((item) =>
          item.id === itemId ? item : { ...item, selected: false },
        );
      }
      return this.toOptionCard(option, items.filter((item) => item.selected).map((item) => item.id));
    });
    const selectedOptionItemIds = options.flatMap((option) =>
      option.items.filter((item) => item.selected).map((item) => item.id),
    );
    this.setData({ options, selectedOptionItemIds });
    this.refreshPreview();
  },

  onPetChange(event: any) {
    this.setData({ petIndex: Number(event.detail.value) });
  },

  onStoreChange(event: any) {
    this.setData({ storeIndex: Number(event.detail.value) });
  },

  async onCouponChange(event: any) {
    this.setData({ couponIndex: Number(event.detail.value) });
    await this.refreshPreview();
  },

  async refreshPreview() {
    const coupon =
      this.data.couponIndex > 0
        ? this.data.couponOptions[this.data.couponIndex - 1]
        : undefined;
    try {
      const preview = await previewPackage(
        this.data.id,
        this.data.selectedOptionItemIds,
        coupon?.id,
      );
      const couponOptions = preview.couponOptions || [];
      let couponIndex = this.data.couponIndex;
      if (couponIndex > couponOptions.length) {
        couponIndex = 0;
      }
      this.setData({
        preview,
        previewItems: (preview.items || []).map((item) => ({
          ...item,
          addPriceText: formatMoney(item.addPrice || 0),
        })),
        couponOptions,
        couponLabels: [
          '不使用优惠券',
          ...couponOptions.map(
            (item) => `${item.templateName} -¥${formatMoney(item.discountAmount)}`,
          ),
        ],
        couponIndex,
        originalText: formatMoney(preview.originalAmount),
        memberDiscountText: formatMoney(preview.memberDiscount),
        couponDiscountText: formatMoney(preview.couponDiscount),
        payableText: formatMoney(preview.payableAmount),
        walletText: formatMoney(preview.walletAvailableBalance),
      });
    } catch (error) {
      // request 已提示
    }
  },

  async submit() {
    if (this.data.submitting) {
      return;
    }
    const pet = this.data.pets[this.data.petIndex];
    if (!pet) {
      wx.showToast({ title: '请先添加宠物', icon: 'none' });
      return;
    }
    const store = this.data.stores[this.data.storeIndex];
    if (!store) {
      wx.showToast({ title: '暂无可购买门店', icon: 'none' });
      return;
    }
    for (const option of this.data.options) {
      const min = option.required === 1 ? Math.max(1, option.minSelect) : option.minSelect;
      if (option.selectedCount < min) {
        wx.showToast({ title: `请选择「${option.optionName}」`, icon: 'none' });
        return;
      }
    }
    const coupon =
      this.data.couponIndex > 0
        ? this.data.couponOptions[this.data.couponIndex - 1]
        : undefined;
    try {
      this.setData({ submitting: true });
      const result = await purchasePackage({
        templateId: this.data.id,
        petId: pet.id,
        storeId: store.id,
        optionItemIds: this.data.selectedOptionItemIds,
        couponId: coupon?.id,
        idempotencyKey: `PKG-${Date.now()}-${Math.floor(Math.random() * 10000)}`,
      });
      if (result.payableAmount <= 0) {
        wx.showToast({ title: '购买成功', icon: 'success' });
        wx.redirectTo({ url: '/pages/commerce/package/mine' });
      } else {
        wx.showToast({ title: '订单已创建，请支付', icon: 'none' });
        wx.redirectTo({
          url: `/pages/commerce/order/detail?orderNo=${result.orderNo}`,
        });
      }
    } catch (error) {
      // request 已提示
    } finally {
      this.setData({ submitting: false });
    }
  },
}, ["pet-placeholder.png"]));
