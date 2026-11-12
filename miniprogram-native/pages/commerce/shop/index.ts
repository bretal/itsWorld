import { withStaticImages } from '../../../utils/static-images';
import { requireLogin } from '../../../utils/auth';
import { listShopCategories, listShopProducts, getCart } from '../../../services/commerce';
import type { ShopCategory, ShopProductItem } from '../../../services/commerce';
import { formatMoney } from '../../../utils/format';
import { resolveImage } from '../../../utils/image';

interface ProductCard extends ShopProductItem {
  coverSrc: string;
  priceText: string;
  memberPriceText: string;
}
const ASSET = '/assets/shop/';
const shortcuts = [
  { name: '猫咪零食', keyword: '猫', icon: '01_01_cat_snack.png' },
  { name: '狗狗零食', keyword: '狗', icon: '01_02_dog_snack.png' },
  { name: '冻干系列', keyword: '冻干', icon: '01_03_freeze_dried_cubes.png' },
  { name: '罐头湿粮', keyword: '罐头', icon: '01_04_canned_food.png' },
  { name: '磨牙洁齿', keyword: '磨牙', icon: '01_05_paw_cookie.png' },
];

Page(withStaticImages({
  data: {
    asset: ASSET,
    shortcuts,
    benefits: [
      { icon: '02_01_delivery_truck.png', name: '配送到家' },
      { icon: '02_02_quality_shield.png', name: '用心甄选' },
      { icon: '02_03_natural_leaf.png', name: '营养好物' },
      { icon: '02_04_fast_lightning.png', name: '便捷选购' },
    ],
    categories: [] as ShopCategory[],
    activeCategoryId: 0,
    keyword: '',
    searchInput: '',
    activeShortcut: '',
    products: [] as ProductCard[],
    recommendations: [] as ProductCard[],
    cartCount: 0,
    loading: true,
    loadingMore: false,
    loadError: false,
    page: 1,
    size: 10,
    hasMore: true,
  },
  _requestId: 0,
  _alive: true,
  onLoad() {
    if (!requireLogin()) return;
    this.loadCategories();
    this.loadRecommendations();
    this.loadProducts(true);
  },
  onShow() {
    if (!requireLogin()) return;
    this.loadCartCount();
  },
  onUnload() {
    this._alive = false;
    this._requestId++;
  },
  onPullDownRefresh() {
    Promise.all([this.loadProducts(true), this.loadRecommendations(), this.loadCategories(), this.loadCartCount()])
      .finally(() => wx.stopPullDownRefresh());
  },
  onReachBottom() {
    if (this.data.hasMore && !this.data.loading && !this.data.loadingMore) this.loadProducts(false);
  },
  async loadCartCount() {
    try {
      const items = await getCart();
      if (this._alive) this.setData({ cartCount: items.reduce((sum, item) => sum + item.quantity, 0) });
    } catch (_) { /* 请求层统一处理错误。 */ }
  },
  async loadCategories() {
    try {
      const categories = await listShopCategories();
      if (this._alive) this.setData({ categories });
    } catch (_) { /* 分类失败不阻塞商品列表。 */ }
  },
  async toCards(records: ShopProductItem[]): Promise<ProductCard[]> {
    return Promise.all(records.map(async product => {
      const price = product.minPrice;
      const memberPrice = product.minMemberPrice;
      return {
        ...product,
        coverSrc: await resolveImage(product.coverUrl),
        priceText: price == null ? '待定' : formatMoney(price),
        memberPriceText: price != null && memberPrice != null && memberPrice < price
          ? `会员价 ¥${formatMoney(memberPrice)}` : '',
      };
    }));
  },
  async loadRecommendations() {
    try {
      const result = await listShopProducts(undefined, undefined, 1, 3);
      const recommendations = await this.toCards(result.records || []);
      if (this._alive) this.setData({ recommendations });
    } catch (_) { /* 推荐区失败时隐藏，主列表可独立重试。 */ }
  },
  async loadProducts(reset: boolean) {
    const requestId = ++this._requestId;
    const page = reset ? 1 : this.data.page + 1;
    this.setData(reset ? { loading: true, loadingMore: false, loadError: false, products: [] }
      : { loadingMore: true, loadError: false });
    try {
      const result = await listShopProducts(this.data.activeCategoryId || undefined,
        this.data.keyword || undefined, page, this.data.size);
      const products = await this.toCards(result.records || []);
      // 快速切换分类/搜索时，旧请求不能覆盖当前结果。
      if (!this._alive || requestId !== this._requestId) return;
      this.setData({
        products: reset ? products : this.data.products.concat(products),
        page: result.pageNum,
        hasMore: result.pageNum < result.totalPages,
        loading: false,
        loadingMore: false,
      });
    } catch (_) {
      if (this._alive && requestId === this._requestId) {
        this.setData({ loading: false, loadingMore: false, loadError: true });
      }
    }
  },
  onSearchInput(event: any) { this.setData({ searchInput: event.detail.value }); },
  onSearch() {
    this.setData({ keyword: this.data.searchInput.trim(), activeShortcut: '' });
    this.loadProducts(true);
    this.scrollToProducts();
  },
  clearSearch() {
    this.setData({ keyword: '', searchInput: '', activeShortcut: '' });
    this.loadProducts(true);
  },
  onTapShortcut(event: any) {
    const index = Number(event.currentTarget.dataset.index);
    const shortcut = shortcuts[index];
    if (!shortcut) return;
    const category = this.data.categories.find((item: ShopCategory) => item.name.includes(shortcut.keyword));
    this.setData({ activeShortcut: shortcut.name, activeCategoryId: category?.id || 0,
      keyword: category ? '' : shortcut.keyword, searchInput: category ? '' : shortcut.keyword });
    this.loadProducts(true);
    this.scrollToProducts();
  },
  onTapCategory(event: any) {
    this.setData({ activeCategoryId: Number(event.currentTarget.dataset.id) || 0, activeShortcut: '' });
    this.loadProducts(true);
  },
  scrollToProducts() { wx.pageScrollTo({ selector: '#curated', duration: 250 }); },
  viewAll() {
    this.setData({ activeCategoryId: 0, keyword: '', searchInput: '', activeShortcut: '' });
    this.loadProducts(true);
    this.scrollToProducts();
  },
  retry() { this.loadProducts(!this.data.products.length); },
  goProductDetail(event: any) {
    wx.navigateTo({ url: `/pages/commerce/shop/detail?id=${Number(event.currentTarget.dataset.id)}` });
  },

  goCart() { wx.navigateTo({ url: '/pages/commerce/cart/index' }); },
  onImageError(event: any) {
    const id = Number(event.currentTarget.dataset.id);
    this.setData({
      products: this.data.products.map((item: ProductCard) => item.id === id ? { ...item, coverSrc: '' } : item),
      recommendations: this.data.recommendations.map((item: ProductCard) => item.id === id ? { ...item, coverSrc: '' } : item),
    });
  },
}, ['pet-placeholder.png']));
