package com.itsworld.commerce.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsworld.commerce.address.entity.UserShippingAddress;
import com.itsworld.commerce.address.service.AddressService;
import com.itsworld.commerce.cart.entity.ShopCartItem;
import com.itsworld.commerce.cart.service.CartService;
import com.itsworld.commerce.catalog.entity.ShopProduct;
import com.itsworld.commerce.catalog.entity.ShopSku;
import com.itsworld.commerce.catalog.mapper.ShopProductMapper;
import com.itsworld.commerce.catalog.mapper.ShopSkuMapper;
import com.itsworld.commerce.common.CommercePriceUtils;
import com.itsworld.commerce.inventory.entity.ShopInventory;
import com.itsworld.commerce.inventory.mapper.ShopInventoryMapper;
import com.itsworld.commerce.inventory.service.InventoryService;
import com.itsworld.commerce.order.dto.CheckoutItemView;
import com.itsworld.commerce.order.dto.CheckoutPreviewRequest;
import com.itsworld.commerce.order.dto.CheckoutPreviewResponse;
import com.itsworld.commerce.order.dto.CheckoutSubmitRequest;
import com.itsworld.commerce.order.dto.OrderSubmitResponse;
import com.itsworld.commerce.order.entity.CommerceOrder;
import com.itsworld.commerce.order.entity.CommerceOrderDiscount;
import com.itsworld.commerce.order.entity.CommerceOrderItem;
import com.itsworld.commerce.order.entity.CommerceOrderStatusLog;
import com.itsworld.commerce.order.mapper.CommerceOrderDiscountMapper;
import com.itsworld.commerce.order.mapper.CommerceOrderItemMapper;
import com.itsworld.commerce.order.mapper.CommerceOrderMapper;
import com.itsworld.commerce.order.mapper.CommerceOrderStatusLogMapper;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.ErrorCode;
import com.itsworld.member.dto.CouponOptionView;
import com.itsworld.member.dto.CouponQuote;
import com.itsworld.member.service.CouponService;
import com.itsworld.member.service.MemberService;
import com.itsworld.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 结算业务实现
 *
 * @author Claude
 * @date 2026-11-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private static final long DEFAULT_WAREHOUSE_ID = 0L;
    private static final int PAY_EXPIRE_MINUTES = 15;

    private final CartService cartService;
    private final AddressService addressService;
    private final CouponService couponService;
    private final MemberService memberService;
    private final WalletService walletService;
    private final InventoryService inventoryService;
    private final ShopSkuMapper skuMapper;
    private final ShopProductMapper productMapper;
    private final ShopInventoryMapper inventoryMapper;
    private final CommerceOrderMapper orderMapper;
    private final CommerceOrderItemMapper orderItemMapper;
    private final CommerceOrderDiscountMapper orderDiscountMapper;
    private final CommerceOrderStatusLogMapper orderStatusLogMapper;
    private final ObjectMapper objectMapper;

    public CheckoutPreviewResponse preview(Long userId, CheckoutPreviewRequest request) {
        List<ShopCartItem> cartItems = cartService.listSelectedItems(userId);
        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "请先选择要结算的商品");
        }
        BigDecimal discountRate = memberService.getCurrentDiscountRate(userId);
        List<CheckoutItemView> items = new ArrayList<>();
        long originalAmount = 0L;
        long memberDiscount = 0L;
        long memberTotal = 0L;
        boolean valid = true;

        for (ShopCartItem cartItem : cartItems) {
            ShopSku sku = skuMapper.selectById(cartItem.getSkuId());
            ShopProduct product = sku != null ? productMapper.selectById(sku.getProductId()) : null;
            ShopInventory inventory = sku != null
                    ? inventoryMapper.selectBySkuIdAndWarehouse(sku.getId(), DEFAULT_WAREHOUSE_ID)
                    : null;
            int available = inventory != null && inventory.getAvailableQty() != null
                    ? inventory.getAvailableQty() : 0;
            String invalidReason = resolveInvalidReason(sku, product, cartItem.getQuantity(), available);
            if (invalidReason != null) {
                valid = false;
            }
            long unitPrice = sku != null && sku.getSalePrice() != null ? sku.getSalePrice() : 0L;
            long memberUnitPrice = CommercePriceUtils.applyDiscount(unitPrice, discountRate);
            long itemOriginal = unitPrice * cartItem.getQuantity();
            long itemMemberTotal = memberUnitPrice * cartItem.getQuantity();
            long itemDiscount = itemOriginal - itemMemberTotal;
            originalAmount += itemOriginal;
            memberDiscount += itemDiscount;
            memberTotal += itemMemberTotal;
            items.add(CheckoutItemView.builder()
                    .cartItemId(cartItem.getId())
                    .skuId(cartItem.getSkuId())
                    .productId(sku != null ? sku.getProductId() : null)
                    .productName(product != null ? product.getName() : "商品已失效")
                    .productCoverUrl(product != null ? product.getCoverUrl() : null)
                    .skuCode(sku != null ? sku.getSkuCode() : null)
                    .specJson(sku != null ? sku.getSpecJson() : null)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(unitPrice)
                    .memberUnitPrice(memberUnitPrice)
                    .originalTotal(itemOriginal)
                    .memberDiscount(itemDiscount)
                    .payableTotal(itemMemberTotal)
                    .availableQty(available)
                    .invalidReason(invalidReason)
                    .build());
        }

        CouponQuote couponQuote = null;
        long couponDiscount = 0L;
        if (request != null && request.getCouponId() != null) {
            couponQuote = couponService.quoteCoupon(userId, request.getCouponId(), memberTotal);
            couponDiscount = couponQuote.getDiscountAmount();
        }
        long payableAmount = Math.max(0L, memberTotal - couponDiscount);
        List<CouponOptionView> couponOptions = couponService.listCouponOptions(userId, memberTotal);

        return CheckoutPreviewResponse.builder()
                .items(items)
                .originalAmount(originalAmount)
                .memberDiscount(memberDiscount)
                .couponDiscount(couponDiscount)
                .shippingFee(0L)
                .payableAmount(payableAmount)
                .couponOptions(couponOptions)
                .walletAvailableBalance(walletService.getSummary(userId).getAvailableBalance())
                .valid(valid)
                .build();
    }

    @Transactional
    public OrderSubmitResponse submit(Long userId, CheckoutSubmitRequest request) {
        CommerceOrder existing = orderMapper.selectByUserAndIdempotency(
                userId, request.getIdempotencyKey());
        if (existing != null) {
            return OrderSubmitResponse.builder()
                    .orderId(existing.getId())
                    .orderNo(existing.getOrderNo())
                    .payableAmount(existing.getPayableAmount())
                    .expireTime(existing.getExpireTime())
                    .build();
        }

        CheckoutPreviewResponse preview = preview(userId,
                CheckoutPreviewRequest.builder().couponId(request.getCouponId()).build());
        if (!Boolean.TRUE.equals(preview.getValid())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "购物车存在失效商品，请先处理");
        }
        if (preview.getPayableAmount() == null || preview.getPayableAmount() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "订单应付金额必须大于0");
        }
        UserShippingAddress address = addressService.getOwnedAddress(userId, request.getAddressId());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = now.plusMinutes(PAY_EXPIRE_MINUTES);
        String orderNo = generateOrderNo();
        long couponDiscount = preview.getCouponDiscount() != null ? preview.getCouponDiscount() : 0L;
        Map<Long, Long> couponShares = allocateCoupon(preview.getItems(), couponDiscount);

        CommerceOrder order = CommerceOrder.builder()
                .orderNo(orderNo)
                .userId(userId)
                .orderType("PRODUCT")
                .orderStatus("WAIT_PAY")
                .paymentStatus("UNPAID")
                .fulfillmentStatus("PENDING")
                .originalAmount(preview.getOriginalAmount())
                .discountAmount(preview.getMemberDiscount() + couponDiscount)
                .payableAmount(preview.getPayableAmount())
                .paidAmount(0L)
                .refundedAmount(0L)
                .couponId(request.getCouponId())
                .addressSnapshot(toAddressSnapshot(address))
                .expireTime(expireTime)
                .remark(request.getRemark())
                .idempotencyKey(request.getIdempotencyKey())
                .createdAt(now)
                .updatedAt(now)
                .build();
        orderMapper.insert(order);

        BigDecimal discountRate = memberService.getCurrentDiscountRate(userId);
        for (CheckoutItemView item : preview.getItems()) {
            long couponShare = couponShares.getOrDefault(item.getCartItemId(), 0L);
            long discountAllocated = item.getMemberDiscount() + couponShare;
            CommerceOrderItem orderItem = CommerceOrderItem.builder()
                    .orderId(order.getId())
                    .itemType("PRODUCT")
                    .targetId(item.getProductId())
                    .skuId(item.getSkuId())
                    .quantity(item.getQuantity())
                    .titleSnapshot(item.getProductName())
                    .specSnapshot(item.getSpecJson())
                    .unitPrice(item.getUnitPrice())
                    .originalTotal(item.getOriginalTotal())
                    .discountAllocated(discountAllocated)
                    .payableTotal(item.getOriginalTotal() - discountAllocated)
                    .fulfilledQty(0)
                    .refundedAmount(0L)
                    .createdAt(now)
                    .build();
            orderItemMapper.insert(orderItem);

            if (item.getMemberDiscount() > 0) {
                orderDiscountMapper.insert(CommerceOrderDiscount.builder()
                        .orderId(order.getId())
                        .orderItemId(orderItem.getId())
                        .discountType("MEMBER")
                        .discountAmount(item.getMemberDiscount())
                        .ruleSnapshot(toJson(Map.of("discountRate", discountRate)))
                        .createdAt(now)
                        .build());
            }
            inventoryService.reserveStock(order.getId(), orderItem.getId(),
                    item.getSkuId(), item.getQuantity(), expireTime);
        }

        if (request.getCouponId() != null) {
            couponService.lockCoupon(userId, request.getCouponId(), orderNo);
            if (couponDiscount > 0) {
                orderDiscountMapper.insert(CommerceOrderDiscount.builder()
                        .orderId(order.getId())
                        .orderItemId(null)
                        .discountType("COUPON")
                        .sourceId(request.getCouponId())
                        .discountAmount(couponDiscount)
                        .ruleSnapshot(toJson(Map.of("couponId", request.getCouponId())))
                        .createdAt(now)
                        .build());
            }
        }

        cartService.deleteSelectedItems(userId);
        orderStatusLogMapper.insert(CommerceOrderStatusLog.builder()
                .orderId(order.getId())
                .fromStatus(null)
                .toStatus("WAIT_PAY")
                .action("CREATE")
                .operatorType("USER")
                .operatorId(userId)
                .remark("提交订单")
                .createdAt(now)
                .build());

        log.info("订单创建: orderNo={}, userId={}, payable={}", orderNo, userId, order.getPayableAmount());
        return OrderSubmitResponse.builder()
                .orderId(order.getId())
                .orderNo(orderNo)
                .payableAmount(order.getPayableAmount())
                .expireTime(expireTime)
                .build();
    }

    private String resolveInvalidReason(ShopSku sku, ShopProduct product, int quantity, int available) {
        if (sku == null || !"ENABLED".equals(sku.getStatus())) {
            return "SKU已下架";
        }
        if (product == null || !"ON_SALE".equals(product.getSaleStatus())) {
            return "商品已下架";
        }
        if (available < quantity) {
            return "库存不足，剩余 " + available + " 件";
        }
        return null;
    }

    private Map<Long, Long> allocateCoupon(List<CheckoutItemView> items, long couponDiscount) {
        Map<Long, Long> result = new LinkedHashMap<>();
        if (couponDiscount <= 0 || items.isEmpty()) {
            return result;
        }
        long totalWeight = items.stream().mapToLong(CheckoutItemView::getPayableTotal).sum();
        if (totalWeight <= 0) {
            return result;
        }
        long allocated = 0L;
        List<RemainderItem> remainders = new ArrayList<>();
        for (CheckoutItemView item : items) {
            long exact = BigDecimal.valueOf(couponDiscount)
                    .multiply(BigDecimal.valueOf(item.getPayableTotal()))
                    .divide(BigDecimal.valueOf(totalWeight), 0, RoundingMode.DOWN)
                    .longValue();
            result.put(item.getCartItemId(), exact);
            allocated += exact;
            long remainder = couponDiscount * item.getPayableTotal() % totalWeight;
            remainders.add(new RemainderItem(item.getCartItemId(), remainder));
        }
        long left = couponDiscount - allocated;
        remainders.sort(Comparator.comparingLong(RemainderItem::remainder).reversed());
        int index = 0;
        while (left > 0 && !remainders.isEmpty()) {
            Long cartItemId = remainders.get(index % remainders.size()).cartItemId();
            result.put(cartItemId, result.getOrDefault(cartItemId, 0L) + 1);
            left--;
            index++;
        }
        return result;
    }

    private String toAddressSnapshot(UserShippingAddress address) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("recipientName", address.getRecipientName());
        snapshot.put("mobile", address.getMobile());
        snapshot.put("province", address.getProvince());
        snapshot.put("city", address.getCity());
        snapshot.put("district", address.getDistrict());
        snapshot.put("addressDetail", address.getAddressDetail());
        return toJson(snapshot);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("序列化快照失败", e);
            return null;
        }
    }

    private String generateOrderNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "ORD" + time + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8).toUpperCase();
    }

    private record RemainderItem(Long cartItemId, long remainder) {
    }

}
