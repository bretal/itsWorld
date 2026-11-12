import { request } from '../utils/request';
import type { PageData } from '../types/page';

export interface CommerceStore {
  id: number;
  storeCode: string;
  name: string;
  phone?: string;
  province?: string;
  city?: string;
  district?: string;
  address?: string;
  businessStatus: string;
  description?: string;
}

export interface CommerceStoreBusinessHour {
  id: number;
  storeId: number;
  dayOfWeek: number;
  openTime: string;
  closeTime: string;
  specialDate?: string;
  closed: number;
}

export interface CommerceStoreDetail extends CommerceStore {
  hours: CommerceStoreBusinessHour[];
}

export interface GroomingCategory {
  id: number;
  name: string;
  iconUrl?: string;
  sort?: number;
  status?: string;
}

export interface GroomingServiceItem {
  id: number;
  categoryId?: number;
  categoryName?: string;
  serviceCode: string;
  name: string;
  coverUrl?: string;
  description?: string;
  durationMin: number;
  basePrice: number;
  storePrice?: number;
  memberPrice?: number;
  status: string;
  sort?: number;
}

export interface GroomingPetRule {
  id: number;
  serviceId: number;
  speciesCode: string;
  minWeightGram?: number;
  maxWeightGram?: number;
  minAgeMonth?: number;
  maxAgeMonth?: number;
  note?: string;
}

export interface GroomingServiceDetail extends GroomingServiceItem {
  petRules: GroomingPetRule[];
  petEligible?: boolean;
  ineligibleReason?: string;
}

export interface ShopCategory {
  id: number;
  parentId?: number;
  name: string;
  iconUrl?: string;
  sort?: number;
  status?: string;
}

export interface ShopProductItem {
  id: number;
  productCode: string;
  categoryId?: number;
  categoryName?: string;
  name: string;
  subtitle?: string;
  coverUrl?: string;
  saleStatus: string;
  minPrice?: number;
  minMemberPrice?: number;
  sort?: number;
}

export interface ShopSkuItem {
  id: number;
  productId: number;
  skuCode: string;
  specJson?: string;
  salePrice: number;
  marketPrice: number;
  memberPrice?: number;
  weightGram?: number;
  status: string;
  availableQty: number;
}

export interface ShopProductImage {
  id: number;
  productId: number;
  imageUrl: string;
  sort: number;
}

export interface ShopProductDetail {
  id: number;
  productCode: string;
  categoryId?: number;
  categoryName?: string;
  name: string;
  subtitle?: string;
  description?: string;
  coverUrl?: string;
  saleStatus: string;
  sort?: number;
  images: ShopProductImage[];
  skus: ShopSkuItem[];
}

export function listStores(page = 1, size = 10, keyword?: string) {
  return request<PageData<CommerceStore>>({
    url: '/commerce/stores',
    data: { page, size, keyword },
  });
}

export function getStoreDetail(id: number) {
  return request<CommerceStoreDetail>({ url: `/commerce/stores/${id}` });
}

export function listGroomingCategories() {
  return request<GroomingCategory[]>({ url: '/commerce/grooming/categories' });
}

export function listGroomingServices(
  storeId?: number,
  categoryId?: number,
  page = 1,
  size = 10,
  keyword?: string,
) {
  return request<PageData<GroomingServiceItem>>({
    url: '/commerce/grooming/services',
    data: { storeId, categoryId, page, size, keyword },
  });
}

export function getGroomingService(
  id: number,
  storeId?: number,
  petId?: number,
) {
  return request<GroomingServiceDetail>({
    url: `/commerce/grooming/services/${id}`,
    data: { storeId, petId },
  });
}

export function listShopCategories() {
  return request<ShopCategory[]>({ url: '/commerce/shop/categories' });
}

export function listShopProducts(
  categoryId?: number,
  keyword?: string,
  page = 1,
  size = 10,
) {
  return request<PageData<ShopProductItem>>({
    url: '/commerce/shop/products',
    data: { categoryId, keyword, page, size },
  });
}

export function getShopProduct(id: number | string) {
  return request<ShopProductDetail>({ url: `/commerce/shop/products/${id}` });
}

// ---------- 地址 ----------

export interface ShippingAddress {
  id: number;
  userId: number;
  recipientName: string;
  mobile: string;
  province?: string;
  city?: string;
  district?: string;
  addressDetail: string;
  isDefault: number;
}

export interface AddressSaveRequest {
  recipientName: string;
  mobile: string;
  province?: string;
  city?: string;
  district?: string;
  addressDetail: string;
  isDefault?: number;
}

export function listAddresses() {
  return request<ShippingAddress[]>({ url: '/commerce/addresses' });
}

export function createAddress(data: AddressSaveRequest) {
  return request<ShippingAddress>({
    url: '/commerce/addresses',
    method: 'POST',
    data: data as any,
  });
}

export function updateAddress(id: number, data: AddressSaveRequest) {
  return request<ShippingAddress>({
    url: `/commerce/addresses/${id}`,
    method: 'PUT',
    data: data as any,
  });
}

export function deleteAddress(id: number) {
  return request<void>({ url: `/commerce/addresses/${id}`, method: 'DELETE' });
}

export function setDefaultAddress(id: number) {
  return request<void>({ url: `/commerce/addresses/${id}/default`, method: 'PUT' });
}

// ---------- 购物车 ----------

export interface CartItem {
  id: number;
  skuId: number;
  productId?: number;
  productName: string;
  productCoverUrl?: string;
  skuCode?: string;
  specJson?: string;
  salePrice: number;
  memberPrice: number;
  quantity: number;
  selected: number;
  availableQty: number;
  invalidReason?: string;
}

export function getCart() {
  return request<CartItem[]>({ url: '/commerce/cart' });
}

export function addCartItem(skuId: number, quantity: number) {
  return request<void>({
    url: '/commerce/cart/items',
    method: 'POST',
    data: { skuId, quantity },
  });
}

export function updateCartItem(id: number, data: { quantity?: number; selected?: number }) {
  return request<void>({
    url: `/commerce/cart/items/${id}`,
    method: 'PUT',
    data: data as any,
  });
}

export function removeCartItem(id: number) {
  return request<void>({ url: `/commerce/cart/items/${id}`, method: 'DELETE' });
}

// ---------- 结算 ----------

export interface CouponOption {
  id: number;
  templateName: string;
  couponType: string;
  amountCents?: number;
  discountRate?: number;
  minAmountCents?: number;
  discountAmount: number;
  description?: string;
}

export interface CheckoutItem {
  cartItemId: number;
  skuId: number;
  productId?: number;
  productName: string;
  productCoverUrl?: string;
  skuCode?: string;
  specJson?: string;
  quantity: number;
  unitPrice: number;
  memberUnitPrice: number;
  originalTotal: number;
  memberDiscount: number;
  payableTotal: number;
  availableQty: number;
  invalidReason?: string;
}

export interface CheckoutPreview {
  items: CheckoutItem[];
  originalAmount: number;
  memberDiscount: number;
  couponDiscount: number;
  shippingFee: number;
  payableAmount: number;
  couponOptions: CouponOption[];
  walletAvailableBalance: number;
  valid: boolean;
}

export interface CheckoutSubmitRequest {
  idempotencyKey: string;
  couponId?: number;
  addressId: number;
  remark?: string;
}

export interface OrderSubmitResult {
  orderId: number;
  orderNo: string;
  payableAmount: number;
  expireTime: string;
}

export function previewCheckout(couponId?: number) {
  return request<CheckoutPreview>({
    url: '/commerce/checkout/preview',
    method: 'POST',
    data: { couponId },
  });
}

export function submitCheckout(data: CheckoutSubmitRequest) {
  return request<OrderSubmitResult>({
    url: '/commerce/checkout/submit',
    method: 'POST',
    data: data as any,
  });
}

// ---------- 订单/支付 ----------

export interface OrderItem {
  id: number;
  itemType: string;
  targetId?: number;
  skuId?: number;
  quantity: number;
  titleSnapshot: string;
  specSnapshot?: string;
  unitPrice: number;
  originalTotal: number;
  discountAllocated: number;
  payableTotal: number;
}

export interface PaymentRecord {
  id: number;
  paymentOrderId: string;
  payChannel: string;
  payAmount: number;
  status: string;
  externalTxNo?: string;
  paidTime?: string;
}

export interface OrderDetail {
  id: number;
  orderNo: string;
  userId: number;
  orderType: string;
  orderStatus: string;
  paymentStatus: string;
  fulfillmentStatus: string;
  originalAmount: number;
  discountAmount: number;
  payableAmount: number;
  paidAmount: number;
  refundedAmount: number;
  couponId?: number;
  addressSnapshot?: string;
  expireTime?: string;
  paidTime?: string;
  closeTime?: string;
  cancelReason?: string;
  remark?: string;
  createdAt: string;
  items: OrderItem[];
  discounts: any[];
  latestPayment?: PaymentRecord;
}

export interface PaymentResult {
  orderNo: string;
  paymentOrderId?: string;
  payChannel?: string;
  payAmount: number;
  status: string;
  externalTxNo?: string;
  paidTime?: string;
  orderStatus: string;
  paymentStatus: string;
}

export function listOrders(status?: string, page = 1, size = 10) {
  return request<PageData<OrderDetail>>({
    url: '/commerce/orders',
    data: { status, page, size },
  });
}

export function getOrder(orderNo: string) {
  return request<OrderDetail>({ url: `/commerce/orders/${orderNo}` });
}

export function cancelOrder(orderNo: string, reason?: string) {
  return request<void>({
    url: `/commerce/orders/${orderNo}/cancel`,
    method: 'POST',
    data: { reason },
  });
}

export function payOrder(orderNo: string, channel: string) {
  return request<PaymentResult>({
    url: `/commerce/orders/${orderNo}/payments`,
    method: 'POST',
    data: { channel },
  });
}

export function getPaymentStatus(orderNo: string) {
  return request<PaymentResult>({
    url: `/commerce/orders/${orderNo}/payment-status`,
  });
}

// ---------- 预约 ----------

export interface BookingSlot {
  id: number;
  storeId: number;
  staffId: number;
  serviceDate: string;
  startTime: string;
  endTime: string;
  totalCapacity: number;
  reservedCapacity: number;
  lockedCapacity: number;
  availableCapacity: number;
  status: string;
}

export interface PackageBookingOption {
  userPackageItemId: number;
  userPackageId: number;
  packageInstanceNo: string;
  packageName: string;
  petId?: number;
  petName?: string;
  storeId?: number;
  storeName?: string;
  serviceId: number;
  serviceName?: string;
  totalTimes: number;
  usedTimes: number;
  frozenTimes: number;
  remainingTimes: number;
  expireTime?: string;
}

export interface BookingCreateRequest {
  storeId: number;
  serviceId: number;
  petId: number;
  slotId: number;
  payType: 'ONLINE' | 'PACKAGE';
  userPackageItemId?: number;
  couponId?: number;
  idempotencyKey?: string;
}

export interface BookingCreateResult {
  bookingId: number;
  bookingNo: string;
  status: string;
  orderId?: number;
  orderNo?: string;
  payableAmount?: number;
  expireTime?: string;
}

export interface BookingLog {
  id: number;
  bookingId: number;
  fromStatus?: string;
  toStatus: string;
  action: string;
  operatorType: string;
  operatorId?: number;
  remark?: string;
  createdAt: string;
}

export interface BookingView {
  id: number;
  bookingNo: string;
  userId: number;
  username?: string;
  petId: number;
  petName?: string;
  storeId: number;
  storeName?: string;
  staffId?: number;
  slotId: number;
  serviceDate?: string;
  startTime?: string;
  endTime?: string;
  orderId?: number;
  orderNo?: string;
  serviceId: number;
  serviceName?: string;
  status: string;
  reserveExpireTime?: string;
  checkinTime?: string;
  completedTime?: string;
  cancelReason?: string;
  createdAt: string;
  logs?: BookingLog[];
}

export function listBookingSlots(storeId: number, date?: string) {
  return request<BookingSlot[]>({
    url: '/commerce/booking/slots',
    data: { storeId, date },
  });
}

export function listPackageBookingOptions(serviceId: number, storeId?: number) {
  return request<PackageBookingOption[]>({
    url: '/commerce/booking/package-options',
    data: { serviceId, storeId },
  });
}

export function createBooking(data: BookingCreateRequest) {
  return request<BookingCreateResult>({
    url: '/commerce/bookings',
    method: 'POST',
    data: data as any,
  });
}

export function listBookings(status?: string, page = 1, size = 10) {
  return request<PageData<BookingView>>({
    url: '/commerce/bookings',
    data: { status, page, size },
  });
}

export function getBooking(bookingNo: string) {
  return request<BookingView>({ url: `/commerce/bookings/${bookingNo}` });
}

export function cancelBooking(bookingNo: string, reason?: string) {
  return request<void>({
    url: `/commerce/bookings/${bookingNo}/cancel`,
    method: 'POST',
    data: { reason },
  });
}

export function rescheduleBooking(bookingNo: string, slotId: number) {
  return request<void>({
    url: `/commerce/bookings/${bookingNo}/reschedule`,
    method: 'POST',
    data: { slotId },
  });
}

// ---------- 套餐 ----------

export interface PackageItemView {
  id: number;
  serviceId: number;
  serviceName?: string;
  includedTimes: number;
  extraPrice?: number;
  selectableGroup?: string;
}

export interface PackageOptionItemView {
  id: number;
  serviceId: number;
  serviceName?: string;
  addPrice: number;
  includedTimes: number;
  status: string;
}

export interface PackageOptionView {
  id: number;
  optionCode: string;
  optionName: string;
  minSelect: number;
  maxSelect: number;
  required: number;
  items: PackageOptionItemView[];
}

export interface PackageTemplate {
  id: number;
  packageCode: string;
  name: string;
  coverUrl?: string;
  description?: string;
  salePrice: number;
  validDays?: number;
  status: string;
  allowCrossStore: number;
  items: PackageItemView[];
  options: PackageOptionView[];
}

export interface UserPackageItemView {
  id: number;
  serviceId: number;
  serviceName?: string;
  totalTimes: number;
  usedTimes: number;
  frozenTimes: number;
  remainingTimes: number;
  serviceSnapshot?: string;
}

export interface UserPackageView {
  id: number;
  packageInstanceNo: string;
  packageId: number;
  packageName: string;
  petId?: number;
  petName?: string;
  storeId?: number;
  storeName?: string;
  status: string;
  startTime: string;
  expireTime?: string;
  selectionSnapshot?: string;
  items: UserPackageItemView[];
}

export interface PackageUsageView {
  id: number;
  usageNo: string;
  userPackageId: number;
  packageItemId: number;
  bookingId?: number;
  bookingNo?: string;
  serviceId: number;
  serviceName?: string;
  qty: number;
  status: string;
  usedTime?: string;
  createdAt: string;
}

export interface PackagePurchasePreview {
  templateId: number;
  packageCode: string;
  packageName: string;
  originalAmount: number;
  memberDiscount: number;
  couponDiscount: number;
  payableAmount: number;
  walletAvailableBalance: number;
  items: PackageSelectionItemView[];
  couponOptions: CouponOption[];
}

export interface PackageSelectionItemView {
  serviceId: number;
  serviceName?: string;
  times: number;
  addPrice?: number;
  fromOption?: boolean;
}

export interface PackagePurchaseResult {
  orderId: number;
  orderNo: string;
  payableAmount: number;
  expireTime: string;
}

export function listPackages(keyword?: string, page = 1, size = 10) {
  return request<PageData<PackageTemplate>>({
    url: '/commerce/packages',
    data: { keyword, page, size },
  });
}

export function getPackage(id: number) {
  return request<PackageTemplate>({ url: `/commerce/packages/${id}` });
}

export function previewPackage(templateId: number, optionItemIds: number[], couponId?: number) {
  return request<PackagePurchasePreview>({
    url: '/commerce/packages/preview',
    method: 'POST',
    data: { templateId, optionItemIds, couponId },
  });
}

export function purchasePackage(data: {
  templateId: number;
  petId: number;
  storeId: number;
  optionItemIds: number[];
  couponId?: number;
  idempotencyKey: string;
}) {
  return request<PackagePurchaseResult>({
    url: '/commerce/packages/purchase',
    method: 'POST',
    data: data as any,
  });
}

export function listMyPackages(status?: string, page = 1, size = 10) {
  return request<PageData<UserPackageView>>({
    url: '/commerce/my-packages',
    data: { status, page, size },
  });
}

export function getMyPackage(userPackageId: number) {
  return request<UserPackageView>({ url: `/commerce/my-packages/${userPackageId}` });
}

export function listPackageUsages(userPackageId: number) {
  return request<PackageUsageView[]>({
    url: `/commerce/my-packages/${userPackageId}/usages`,
  });
}

// ---------- 物流 ----------

export interface ShipmentItemView {
  orderItemId: number;
  skuId?: number;
  titleSnapshot?: string;
  qty: number;
}

export interface ShipmentView {
  id: number;
  shipmentNo: string;
  orderId: number;
  orderNo?: string;
  deliveryCompany: string;
  trackingNo: string;
  status: string;
  shippedTime?: string;
  receivedTime?: string;
  remark?: string;
  items: ShipmentItemView[];
}

export function getShipment(orderNo: string) {
  return request<ShipmentView>({ url: `/commerce/orders/${orderNo}/shipment` });
}

export function confirmReceipt(orderNo: string) {
  return request<void>({
    url: `/commerce/orders/${orderNo}/confirm-receipt`,
    method: 'POST',
  });
}

// ---------- 售后 ----------

export interface AfterSaleApplyItem {
  orderItemId: number;
  qty: number;
}

export interface AfterSaleApplyRequest {
  orderNo: string;
  type: 'REFUND_ONLY' | 'RETURN_REFUND';
  reason: string;
  description?: string;
  evidenceUrls?: string[];
  items: AfterSaleApplyItem[];
}

export interface AfterSaleItemView {
  id: number;
  orderItemId: number;
  skuId?: number;
  titleSnapshot?: string;
  qty: number;
  amount: number;
}

export interface AfterSaleLogView {
  id: number;
  afterSaleId: number;
  fromStatus?: string;
  toStatus: string;
  action: string;
  operatorType: string;
  operatorId?: number;
  remark?: string;
  createdAt: string;
}

export interface RefundLinkView {
  id: number;
  refundNo: string;
  channel: string;
  amount: number;
  status: string;
  externalRefundNo?: string;
}

export interface AfterSaleView {
  id: number;
  afterSaleNo: string;
  orderId: number;
  orderNo?: string;
  userId: number;
  username?: string;
  type: string;
  reason: string;
  description?: string;
  evidenceUrls: string[];
  amount: number;
  status: string;
  reviewRemark?: string;
  reviewedAt?: string;
  refundedAt?: string;
  createdAt: string;
  items: AfterSaleItemView[];
  logs?: AfterSaleLogView[];
  refund?: RefundLinkView;
}

export function applyAfterSale(data: AfterSaleApplyRequest) {
  return request<AfterSaleView>({
    url: '/commerce/after-sales',
    method: 'POST',
    data: data as any,
  });
}

export function listAfterSales(status?: string, page = 1, size = 10) {
  return request<PageData<AfterSaleView>>({
    url: '/commerce/after-sales',
    data: { status, page, size },
  });
}

export function getAfterSale(afterSaleNo: string) {
  return request<AfterSaleView>({ url: `/commerce/after-sales/${afterSaleNo}` });
}

// ---------- 评价 ----------

export interface ReviewView {
  id: number;
  orderId: number;
  orderNo?: string;
  userId: number;
  username?: string;
  rating: number;
  content?: string;
  images: string[];
  status: string;
  reply?: string;
  repliedAt?: string;
  createdAt: string;
}

export function submitReview(data: {
  orderNo: string;
  rating: number;
  content?: string;
  images?: string[];
}) {
  return request<ReviewView>({
    url: '/commerce/reviews',
    method: 'POST',
    data: data as any,
  });
}

export function getOrderReview(orderNo: string) {
  return request<ReviewView | null>({
    url: `/commerce/reviews/order/${orderNo}`,
  });
}
