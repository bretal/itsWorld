// 商业服务（门店/美容/商品/库存）前端类型

export interface CommerceStore {
  id: number;
  storeCode: string;
  name: string;
  phone?: string;
  province?: string;
  city?: string;
  district?: string;
  address?: string;
  longitude?: number;
  latitude?: number;
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

export interface CommerceStaff {
  id: number;
  storeId: number;
  staffName: string;
  mobile?: string;
  roleCode?: string;
  status: string;
}

export interface CommerceStaffShift {
  id: number;
  staffId: number;
  storeId: number;
  shiftDate: string;
  startTime: string;
  endTime: string;
  capacity: number;
  status: string;
}

export interface StoreSaveRequest {
  storeCode: string;
  name: string;
  phone?: string;
  province?: string;
  city?: string;
  district?: string;
  address?: string;
  longitude?: number;
  latitude?: number;
  businessStatus?: string;
  description?: string;
}

export interface BusinessHourSaveRequest {
  dayOfWeek: number;
  openTime: string;
  closeTime: string;
  specialDate?: string;
  closed?: number;
}

export interface StaffSaveRequest {
  staffName: string;
  mobile?: string;
  roleCode?: string;
  status?: string;
}

export interface StaffShiftSaveRequest {
  shiftDate: string;
  startTime: string;
  endTime: string;
  capacity: number;
  status?: string;
}

export interface GroomingCategory {
  id: number;
  name: string;
  iconUrl?: string;
  sort: number;
  status: string;
}

export interface GroomingServiceEntity {
  id: number;
  categoryId?: number;
  serviceCode: string;
  name: string;
  coverUrl?: string;
  description?: string;
  durationMin: number;
  basePrice: number;
  status: string;
  sort: number;
}

export interface GroomingServiceStore {
  id: number;
  serviceId: number;
  storeId: number;
  salePrice: number;
  capacityPerSlot: number;
  status: string;
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

export interface GroomingCategorySaveRequest {
  name: string;
  iconUrl?: string;
  sort?: number;
  status?: string;
}

export interface GroomingServiceSaveRequest {
  categoryId?: number;
  serviceCode: string;
  name: string;
  coverUrl?: string;
  description?: string;
  durationMin: number;
  basePrice: number;
  status?: string;
  sort?: number;
}

export interface GroomingServiceStoreSaveRequest {
  storeId: number;
  salePrice: number;
  capacityPerSlot: number;
  status?: string;
}

export interface GroomingPetRuleSaveRequest {
  speciesCode: string;
  minWeightGram?: number;
  maxWeightGram?: number;
  minAgeMonth?: number;
  maxAgeMonth?: number;
  note?: string;
}

export interface ShopCategory {
  id: number;
  parentId?: number;
  name: string;
  iconUrl?: string;
  sort: number;
  status: string;
}

export interface ShopProductEntity {
  id: number;
  productCode: string;
  categoryId?: number;
  name: string;
  subtitle?: string;
  description?: string;
  coverUrl?: string;
  saleStatus: string;
  sort: number;
}

export interface ShopProductImage {
  id: number;
  productId: number;
  imageUrl: string;
  sort: number;
}

export interface ShopSkuEntity {
  id: number;
  productId: number;
  skuCode: string;
  specJson?: string;
  salePrice: number;
  marketPrice: number;
  weightGram?: number;
  status: string;
  version?: number;
}

export interface ShopSkuView {
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
  sort: number;
  images: ShopProductImage[];
  skus: ShopSkuView[];
}

export interface ShopCategorySaveRequest {
  parentId?: number;
  name: string;
  iconUrl?: string;
  sort?: number;
  status?: string;
}

export interface ShopProductSaveRequest {
  productCode: string;
  categoryId?: number;
  name: string;
  subtitle?: string;
  description?: string;
  coverUrl?: string;
  saleStatus?: string;
  sort?: number;
}

export interface ShopSkuSaveRequest {
  initialStock?: number;
  skuCode: string;
  specJson?: string;
  salePrice: number;
  marketPrice: number;
  weightGram?: number;
  status?: string;
}

export interface InventoryView {
  id: number;
  skuId: number;
  productId: number;
  productName: string;
  productCoverUrl?: string;
  skuCode: string;
  specJson?: string;
  salePrice: number;
  availableQty: number;
  lockedQty: number;
  soldQty: number;
  skuStatus: string;
}

export interface InventoryLogView {
  id: number;
  skuId: number;
  skuCode: string;
  productName: string;
  bizType: string;
  bizId?: number;
  changeAvailable: number;
  changeLocked: number;
  beforeAvailable: number;
  afterAvailable: number;
  idempotencyKey: string;
  remark?: string;
  createdAt: string;
}

export interface InventoryAdjustRequest {
  skuId: number;
  changeQty: number;
  reason: string;
}

export interface CommerceOrderItem {
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
  fulfilledQty: number;
  refundedAmount: number;
}

export interface CommerceOrderDiscount {
  id: number;
  orderId: number;
  orderItemId?: number;
  discountType: string;
  sourceId?: number;
  discountAmount: number;
  ruleSnapshot?: string;
  createdAt?: string;
}

export interface CommercePaymentLink {
  id: number;
  orderId: number;
  paymentOrderId: string;
  payChannel: string;
  payAmount: number;
  status: string;
  idempotencyKey: string;
  externalTxNo?: string;
  paidTime?: string;
  failReason?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface CommerceOrderDetail {
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
  storeSnapshot?: string;
  expireTime?: string;
  paidTime?: string;
  closeTime?: string;
  cancelReason?: string;
  remark?: string;
  createdAt: string;
  items: CommerceOrderItem[];
  discounts: CommerceOrderDiscount[];
  latestPayment?: CommercePaymentLink;
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
  availableCapacity?: number;
  status: string;
  version?: number;
}

export interface BookingSlotSaveRequest {
  storeId: number;
  staffId?: number;
  serviceDate: string;
  startTime: string;
  endTime: string;
  totalCapacity: number;
  status?: string;
}

export interface SlotGenerateRequest {
  storeId: number;
  staffId?: number;
  serviceDate: string;
  startTime: string;
  endTime: string;
  intervalMinutes: number;
  totalCapacity: number;
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

export interface PackageItemRequest {
  serviceId: number;
  includedTimes: number;
  extraPrice?: number;
  selectableGroup?: string;
  sort?: number;
}

export interface PackageOptionItemRequest {
  serviceId: number;
  addPrice?: number;
  includedTimes: number;
  status?: string;
}

export interface PackageOptionRequest {
  optionCode: string;
  optionName: string;
  minSelect: number;
  maxSelect: number;
  required?: number;
  sort?: number;
  items: PackageOptionItemRequest[];
}

export interface PackageTemplateSaveRequest {
  packageCode: string;
  name: string;
  coverUrl?: string;
  description?: string;
  salePrice: number;
  validDays?: number;
  status?: string;
  allowCrossStore?: number;
  items?: PackageItemRequest[];
  options?: PackageOptionRequest[];
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

export interface ShipmentCreateRequest {
  deliveryCompany: string;
  trackingNo: string;
  remark?: string;
}

// ---------- 售后 ----------

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
  afterSaleId: number;
  paymentLinkId: number;
  channel: string;
  amount: number;
  status: string;
  externalRefundNo?: string;
  failReason?: string;
  createdAt: string;
  updatedAt?: string;
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
  reviewedBy?: number;
  reviewedAt?: string;
  refundedAt?: string;
  createdAt: string;
  items: AfterSaleItemView[];
  logs?: AfterSaleLogView[];
  refund?: RefundLinkView;
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
