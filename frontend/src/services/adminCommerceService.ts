import { get, post, put, del } from './api';
import type { PageData } from '@/types/common';
import type {
  BusinessHourSaveRequest,
  CommerceStaff,
  CommerceStaffShift,
  CommerceStore,
  CommerceStoreBusinessHour,
  GroomingCategory,
  GroomingCategorySaveRequest,
  GroomingPetRule,
  GroomingPetRuleSaveRequest,
  GroomingServiceEntity,
  GroomingServiceSaveRequest,
  GroomingServiceStore,
  GroomingServiceStoreSaveRequest,
  InventoryAdjustRequest,
  InventoryLogView,
  InventoryView,
  ShopCategory,
  ShopCategorySaveRequest,
  ShopProductDetail,
  ShopProductEntity,
  ShopProductImage,
  ShopProductSaveRequest,
  ShopSkuEntity,
  ShopSkuSaveRequest,
  StaffSaveRequest,
  StaffShiftSaveRequest,
  StoreSaveRequest,
  CommerceOrderDetail,
  BookingSlot,
  BookingSlotSaveRequest,
  SlotGenerateRequest,
  BookingView,
  PackageTemplate,
  PackageTemplateSaveRequest,
  ShipmentView,
  ShipmentCreateRequest,
  AfterSaleView,
  ReviewView,
} from '@/types/commerce';

// ---------- 门店 ----------

export const getStores = (
  page = 1,
  size = 10,
  keyword?: string
): Promise<PageData<CommerceStore>> =>
  get('/api/admin/commerce/stores', { params: { page, size, keyword } });

export const createStore = (data: StoreSaveRequest): Promise<CommerceStore> =>
  post('/api/admin/commerce/stores', data);

export const updateStore = (storeId: number, data: StoreSaveRequest): Promise<CommerceStore> =>
  put(`/api/admin/commerce/stores/${storeId}`, data);

export const changeStoreStatus = (storeId: number, businessStatus: string): Promise<void> =>
  put(`/api/admin/commerce/stores/${storeId}/status`, undefined, {
    params: { businessStatus },
  });

export const getHours = (storeId: number): Promise<CommerceStoreBusinessHour[]> =>
  get(`/api/admin/commerce/stores/${storeId}/hours`);

export const createHour = (
  storeId: number,
  data: BusinessHourSaveRequest
): Promise<CommerceStoreBusinessHour> =>
  post(`/api/admin/commerce/stores/${storeId}/hours`, data);

export const updateHour = (
  hourId: number,
  data: BusinessHourSaveRequest
): Promise<CommerceStoreBusinessHour> =>
  put(`/api/admin/commerce/stores/hours/${hourId}`, data);

export const deleteHour = (hourId: number): Promise<void> =>
  del(`/api/admin/commerce/stores/hours/${hourId}`);

export const getStaff = (storeId: number): Promise<CommerceStaff[]> =>
  get(`/api/admin/commerce/stores/${storeId}/staff`);

export const createStaff = (storeId: number, data: StaffSaveRequest): Promise<CommerceStaff> =>
  post(`/api/admin/commerce/stores/${storeId}/staff`, data);

export const updateStaff = (staffId: number, data: StaffSaveRequest): Promise<CommerceStaff> =>
  put(`/api/admin/commerce/stores/staff/${staffId}`, data);

export const changeStaffStatus = (staffId: number, status: string): Promise<void> =>
  put(`/api/admin/commerce/stores/staff/${staffId}/status`, undefined, { params: { status } });

export const getShifts = (
  staffId: number,
  date?: string
): Promise<CommerceStaffShift[]> =>
  get(`/api/admin/commerce/stores/staff/${staffId}/shifts`, { params: { date } });

export const createShift = (
  staffId: number,
  data: StaffShiftSaveRequest
): Promise<CommerceStaffShift> =>
  post(`/api/admin/commerce/stores/staff/${staffId}/shifts`, data);

export const updateShift = (
  shiftId: number,
  data: StaffShiftSaveRequest
): Promise<CommerceStaffShift> =>
  put(`/api/admin/commerce/stores/shifts/${shiftId}`, data);

export const deleteShift = (shiftId: number): Promise<void> =>
  del(`/api/admin/commerce/stores/shifts/${shiftId}`);

// ---------- 美容 ----------

export const getGroomingCategories = (): Promise<GroomingCategory[]> =>
  get('/api/admin/commerce/grooming/categories');

export const createGroomingCategory = (
  data: GroomingCategorySaveRequest
): Promise<GroomingCategory> => post('/api/admin/commerce/grooming/categories', data);

export const updateGroomingCategory = (
  categoryId: number,
  data: GroomingCategorySaveRequest
): Promise<GroomingCategory> =>
  put(`/api/admin/commerce/grooming/categories/${categoryId}`, data);

export const changeGroomingCategoryStatus = (
  categoryId: number,
  status: string
): Promise<void> =>
  put(`/api/admin/commerce/grooming/categories/${categoryId}/status`, undefined, {
    params: { status },
  });

export const getGroomingServices = (
  page = 1,
  size = 10,
  categoryId?: number,
  keyword?: string
): Promise<PageData<GroomingServiceEntity>> =>
  get('/api/admin/commerce/grooming/services', {
    params: { page, size, categoryId, keyword },
  });

export const getGroomingService = (serviceId: number): Promise<GroomingServiceEntity> =>
  get(`/api/admin/commerce/grooming/services/${serviceId}`);

export const createGroomingService = (
  data: GroomingServiceSaveRequest
): Promise<GroomingServiceEntity> => post('/api/admin/commerce/grooming/services', data);

export const updateGroomingService = (
  serviceId: number,
  data: GroomingServiceSaveRequest
): Promise<GroomingServiceEntity> =>
  put(`/api/admin/commerce/grooming/services/${serviceId}`, data);

export const changeGroomingServiceStatus = (
  serviceId: number,
  status: string
): Promise<void> =>
  put(`/api/admin/commerce/grooming/services/${serviceId}/status`, undefined, {
    params: { status },
  });

export const getGroomingServiceStores = (
  serviceId: number
): Promise<GroomingServiceStore[]> =>
  get(`/api/admin/commerce/grooming/services/${serviceId}/stores`);

export const createGroomingServiceStore = (
  serviceId: number,
  data: GroomingServiceStoreSaveRequest
): Promise<GroomingServiceStore> =>
  post(`/api/admin/commerce/grooming/services/${serviceId}/stores`, data);

export const updateGroomingServiceStore = (
  id: number,
  data: GroomingServiceStoreSaveRequest
): Promise<GroomingServiceStore> =>
  put(`/api/admin/commerce/grooming/service-stores/${id}`, data);

export const deleteGroomingServiceStore = (id: number): Promise<void> =>
  del(`/api/admin/commerce/grooming/service-stores/${id}`);

export const getGroomingPetRules = (serviceId: number): Promise<GroomingPetRule[]> =>
  get(`/api/admin/commerce/grooming/services/${serviceId}/pet-rules`);

export const createGroomingPetRule = (
  serviceId: number,
  data: GroomingPetRuleSaveRequest
): Promise<GroomingPetRule> =>
  post(`/api/admin/commerce/grooming/services/${serviceId}/pet-rules`, data);

export const updateGroomingPetRule = (
  ruleId: number,
  data: GroomingPetRuleSaveRequest
): Promise<GroomingPetRule> =>
  put(`/api/admin/commerce/grooming/pet-rules/${ruleId}`, data);

export const deleteGroomingPetRule = (ruleId: number): Promise<void> =>
  del(`/api/admin/commerce/grooming/pet-rules/${ruleId}`);

// ---------- 商城 ----------

export const getShopCategories = (): Promise<ShopCategory[]> =>
  get('/api/admin/commerce/shop/categories');

export const createShopCategory = (
  data: ShopCategorySaveRequest
): Promise<ShopCategory> => post('/api/admin/commerce/shop/categories', data);

export const updateShopCategory = (
  categoryId: number,
  data: ShopCategorySaveRequest
): Promise<ShopCategory> => put(`/api/admin/commerce/shop/categories/${categoryId}`, data);

export const changeShopCategoryStatus = (categoryId: number, status: string): Promise<void> =>
  put(`/api/admin/commerce/shop/categories/${categoryId}/status`, undefined, {
    params: { status },
  });

export const getShopProducts = (
  page = 1,
  size = 10,
  categoryId?: number,
  keyword?: string
): Promise<PageData<ShopProductEntity>> =>
  get('/api/admin/commerce/shop/products', {
    params: { page, size, categoryId, keyword },
  });

export const getShopProductDetail = (productId: number): Promise<ShopProductDetail> =>
  get(`/api/admin/commerce/shop/products/${productId}`);

export const createShopProduct = (
  data: ShopProductSaveRequest
): Promise<ShopProductEntity> => post('/api/admin/commerce/shop/products', data);

export const updateShopProduct = (
  productId: number,
  data: ShopProductSaveRequest
): Promise<ShopProductEntity> =>
  put(`/api/admin/commerce/shop/products/${productId}`, data);

export const changeShopProductSaleStatus = (
  productId: number,
  saleStatus: string
): Promise<void> =>
  put(`/api/admin/commerce/shop/products/${productId}/sale-status`, undefined, {
    params: { saleStatus },
  });

export const getShopProductImages = (productId: number): Promise<ShopProductImage[]> =>
  get(`/api/admin/commerce/shop/products/${productId}/images`);

export const addShopProductImage = (
  productId: number,
  imageUrl: string,
  sort = 0
): Promise<ShopProductImage> =>
  post(`/api/admin/commerce/shop/products/${productId}/images`, { imageUrl, sort });

export const deleteShopProductImage = (imageId: number): Promise<void> =>
  del(`/api/admin/commerce/shop/images/${imageId}`);

export const createShopSku = (
  productId: number,
  data: ShopSkuSaveRequest
): Promise<ShopSkuEntity> =>
  post(`/api/admin/commerce/shop/products/${productId}/skus`, data);

export const updateShopSku = (
  skuId: number,
  data: ShopSkuSaveRequest
): Promise<ShopSkuEntity> => put(`/api/admin/commerce/shop/skus/${skuId}`, data);

export const changeShopSkuStatus = (skuId: number, status: string): Promise<void> =>
  put(`/api/admin/commerce/shop/skus/${skuId}/status`, undefined, {
    params: { status },
  });

// ---------- 订单 ----------

export const getOrders = (
  page = 1,
  size = 10,
  status?: string,
  keyword?: string
): Promise<PageData<CommerceOrderDetail>> =>
  get('/api/admin/commerce/orders', { params: { page, size, status, keyword } });

export const getOrder = (orderNo: string): Promise<CommerceOrderDetail> =>
  get(`/api/admin/commerce/orders/${orderNo}`);

export const closeOrder = (orderNo: string, reason?: string): Promise<void> =>
  post(`/api/admin/commerce/orders/${orderNo}/close`, undefined, { params: { reason } });

// ---------- 库存 ----------

export const getInventory = (
  page = 1,
  size = 10,
  keyword?: string
): Promise<PageData<InventoryView>> =>
  get('/api/admin/commerce/inventory', { params: { page, size, keyword } });

export const adjustInventory = (data: InventoryAdjustRequest): Promise<InventoryView> =>
  post('/api/admin/commerce/inventory/adjust', data);

export const getInventoryLogs = (
  page = 1,
  size = 10,
  skuId?: number
): Promise<PageData<InventoryLogView>> =>
  get('/api/admin/commerce/inventory/logs', { params: { page, size, skuId } });

// ---------- 预约 ----------

export const getBookingSlots = (
  page = 1,
  size = 10,
  storeId?: number,
  date?: string,
  status?: string
): Promise<PageData<BookingSlot>> =>
  get('/api/admin/commerce/bookings/slots', {
    params: { page, size, storeId, date, status },
  });

export const createBookingSlot = (data: BookingSlotSaveRequest): Promise<BookingSlot> =>
  post('/api/admin/commerce/bookings/slots', data);

export const updateBookingSlot = (
  slotId: number,
  data: BookingSlotSaveRequest
): Promise<BookingSlot> =>
  put(`/api/admin/commerce/bookings/slots/${slotId}`, data);

export const deleteBookingSlot = (slotId: number): Promise<void> =>
  del(`/api/admin/commerce/bookings/slots/${slotId}`);

export const generateBookingSlots = (data: SlotGenerateRequest): Promise<BookingSlot[]> =>
  post('/api/admin/commerce/bookings/slots/generate', data);

export const getBookings = (
  page = 1,
  size = 10,
  status?: string,
  date?: string,
  storeId?: number,
  keyword?: string
): Promise<PageData<BookingView>> =>
  get('/api/admin/commerce/bookings', {
    params: { page, size, status, date, storeId, keyword },
  });

export const getBooking = (bookingNo: string): Promise<BookingView> =>
  get(`/api/admin/commerce/bookings/${bookingNo}`);

export const checkinBooking = (bookingNo: string): Promise<void> =>
  post(`/api/admin/commerce/bookings/${bookingNo}/checkin`);

export const startBooking = (bookingNo: string): Promise<void> =>
  post(`/api/admin/commerce/bookings/${bookingNo}/start`);

export const completeBooking = (bookingNo: string): Promise<void> =>
  post(`/api/admin/commerce/bookings/${bookingNo}/complete`);

export const cancelBooking = (bookingNo: string, reason?: string): Promise<void> =>
  post(`/api/admin/commerce/bookings/${bookingNo}/cancel`, undefined, {
    params: { reason },
  });

export const rescheduleBooking = (bookingNo: string, slotId: number): Promise<void> =>
  post(`/api/admin/commerce/bookings/${bookingNo}/reschedule`, { slotId });

// ---------- 套餐 ----------

export const getPackageTemplates = (
  page = 1,
  size = 10,
  status?: string,
  keyword?: string
): Promise<PageData<PackageTemplate>> =>
  get('/api/admin/commerce/packages', { params: { page, size, status, keyword } });

export const getPackageTemplate = (templateId: number): Promise<PackageTemplate> =>
  get(`/api/admin/commerce/packages/${templateId}`);

export const createPackageTemplate = (
  data: PackageTemplateSaveRequest
): Promise<PackageTemplate> => post('/api/admin/commerce/packages', data);

export const updatePackageTemplate = (
  templateId: number,
  data: PackageTemplateSaveRequest
): Promise<PackageTemplate> => put(`/api/admin/commerce/packages/${templateId}`, data);

export const changePackageTemplateStatus = (
  templateId: number,
  status: string
): Promise<void> =>
  put(`/api/admin/commerce/packages/${templateId}/status`, undefined, {
    params: { status },
  });

// ---------- 物流 ----------

export const getShipments = (
  page = 1,
  size = 10,
  status?: string,
  keyword?: string
): Promise<PageData<ShipmentView>> =>
  get('/api/admin/commerce/shipments', { params: { page, size, status, keyword } });

export const getShipmentByOrder = (orderNo: string): Promise<ShipmentView> =>
  get(`/api/admin/commerce/shipments/order/${orderNo}`);

export const shipOrder = (
  orderNo: string,
  data: ShipmentCreateRequest
): Promise<ShipmentView> =>
  post(`/api/admin/commerce/shipments/order/${orderNo}`, data);

// ---------- 售后 ----------

export const getAfterSales = (
  page = 1,
  size = 10,
  status?: string,
  keyword?: string
): Promise<PageData<AfterSaleView>> =>
  get('/api/admin/commerce/after-sales', { params: { page, size, status, keyword } });

export const getAfterSale = (afterSaleNo: string): Promise<AfterSaleView> =>
  get(`/api/admin/commerce/after-sales/${afterSaleNo}`);

export const reviewAfterSale = (
  afterSaleNo: string,
  approved: boolean,
  remark?: string
): Promise<AfterSaleView> =>
  post(`/api/admin/commerce/after-sales/${afterSaleNo}/review`, {
    approved,
    remark,
  });

export const executeAfterSaleRefund = (afterSaleNo: string): Promise<AfterSaleView> =>
  post(`/api/admin/commerce/after-sales/${afterSaleNo}/refund`);

// ---------- 评价 ----------

export const getReviews = (
  page = 1,
  size = 10,
  status?: string
): Promise<PageData<ReviewView>> =>
  get('/api/admin/commerce/reviews', { params: { page, size, status } });

export const approveReview = (reviewId: number): Promise<ReviewView> =>
  post(`/api/admin/commerce/reviews/${reviewId}/approve`);

export const hideReview = (reviewId: number): Promise<ReviewView> =>
  post(`/api/admin/commerce/reviews/${reviewId}/hide`);

export const replyReview = (reviewId: number, reply: string): Promise<ReviewView> =>
  post(`/api/admin/commerce/reviews/${reviewId}/reply`, { reply });

export default {
  getStores,
  createStore,
  updateStore,
  changeStoreStatus,
  getHours,
  createHour,
  updateHour,
  deleteHour,
  getStaff,
  createStaff,
  updateStaff,
  changeStaffStatus,
  getShifts,
  createShift,
  updateShift,
  deleteShift,
  getGroomingCategories,
  createGroomingCategory,
  updateGroomingCategory,
  changeGroomingCategoryStatus,
  getGroomingServices,
  getGroomingService,
  createGroomingService,
  updateGroomingService,
  changeGroomingServiceStatus,
  getGroomingServiceStores,
  createGroomingServiceStore,
  updateGroomingServiceStore,
  deleteGroomingServiceStore,
  getGroomingPetRules,
  createGroomingPetRule,
  updateGroomingPetRule,
  deleteGroomingPetRule,
  getShopCategories,
  createShopCategory,
  updateShopCategory,
  changeShopCategoryStatus,
  getShopProducts,
  getShopProductDetail,
  createShopProduct,
  updateShopProduct,
  changeShopProductSaleStatus,
  getShopProductImages,
  addShopProductImage,
  deleteShopProductImage,
  createShopSku,
  updateShopSku,
  changeShopSkuStatus,
  getOrders,
  getOrder,
  closeOrder,
  getInventory,
  adjustInventory,
  getInventoryLogs,
  getBookingSlots,
  createBookingSlot,
  updateBookingSlot,
  deleteBookingSlot,
  generateBookingSlots,
  getBookings,
  getBooking,
  checkinBooking,
  startBooking,
  completeBooking,
  cancelBooking,
  rescheduleBooking,
  getPackageTemplates,
  getPackageTemplate,
  createPackageTemplate,
  updatePackageTemplate,
  changePackageTemplateStatus,
  getShipments,
  getShipmentByOrder,
  shipOrder,
  getAfterSales,
  getAfterSale,
  reviewAfterSale,
  executeAfterSaleRefund,
  getReviews,
  approveReview,
  hideReview,
  replyReview,
};
