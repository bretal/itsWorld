package com.itsworld.commerce;

import com.itsworld.commerce.address.dto.AddressSaveRequest;
import com.itsworld.commerce.address.entity.UserShippingAddress;
import com.itsworld.commerce.address.service.AddressService;
import com.itsworld.commerce.aftersale.dto.AfterSaleApplyItemRequest;
import com.itsworld.commerce.aftersale.dto.AfterSaleApplyRequest;
import com.itsworld.commerce.aftersale.dto.AfterSaleReviewRequest;
import com.itsworld.commerce.aftersale.dto.AfterSaleView;
import com.itsworld.commerce.aftersale.service.AfterSaleService;
import com.itsworld.commerce.booking.dto.BookingCreateRequest;
import com.itsworld.commerce.booking.dto.BookingCreateResult;
import com.itsworld.commerce.booking.dto.BookingRescheduleRequest;
import com.itsworld.commerce.booking.entity.BookingSlot;
import com.itsworld.commerce.booking.mapper.BookingSlotMapper;
import com.itsworld.commerce.booking.mapper.GroomingBookingMapper;
import com.itsworld.commerce.booking.service.BookingService;
import com.itsworld.commerce.cart.dto.CartItemAddRequest;
import com.itsworld.commerce.cart.service.CartService;
import com.itsworld.commerce.catalog.entity.ShopCategory;
import com.itsworld.commerce.catalog.entity.ShopProduct;
import com.itsworld.commerce.catalog.entity.ShopSku;
import com.itsworld.commerce.catalog.mapper.ShopCategoryMapper;
import com.itsworld.commerce.catalog.mapper.ShopProductMapper;
import com.itsworld.commerce.catalog.mapper.ShopSkuMapper;
import com.itsworld.commerce.grooming.entity.GroomingCategory;
import com.itsworld.commerce.grooming.entity.GroomingService;
import com.itsworld.commerce.grooming.entity.GroomingServiceStore;
import com.itsworld.commerce.grooming.mapper.GroomingCategoryMapper;
import com.itsworld.commerce.grooming.mapper.GroomingServiceMapper;
import com.itsworld.commerce.grooming.mapper.GroomingServiceStoreMapper;
import com.itsworld.commerce.inventory.entity.ShopInventory;
import com.itsworld.commerce.inventory.mapper.ShopInventoryMapper;
import com.itsworld.commerce.order.dto.CheckoutPreviewRequest;
import com.itsworld.commerce.order.dto.CheckoutSubmitRequest;
import com.itsworld.commerce.order.dto.OrderDetailView;
import com.itsworld.commerce.order.dto.OrderSubmitResponse;
import com.itsworld.commerce.order.dto.PaymentCreateRequest;
import com.itsworld.commerce.order.entity.CommerceOrder;
import com.itsworld.commerce.order.mapper.CommerceOrderMapper;
import com.itsworld.commerce.order.service.CheckoutService;
import com.itsworld.commerce.order.service.CommerceOrderService;
import com.itsworld.commerce.order.service.CommercePaymentService;
import com.itsworld.commerce.packagebiz.dto.PackageItemRequest;
import com.itsworld.commerce.packagebiz.dto.PackagePurchaseRequest;
import com.itsworld.commerce.packagebiz.dto.PackagePurchaseResult;
import com.itsworld.commerce.packagebiz.dto.PackageTemplateSaveRequest;
import com.itsworld.commerce.packagebiz.dto.PackageTemplateView;
import com.itsworld.commerce.packagebiz.dto.UserPackageView;
import com.itsworld.commerce.packagebiz.service.PackageService;
import com.itsworld.commerce.review.dto.ReviewSubmitRequest;
import com.itsworld.commerce.review.dto.ReviewView;
import com.itsworld.commerce.review.service.ReviewService;
import com.itsworld.commerce.store.entity.CommerceStore;
import com.itsworld.commerce.store.mapper.CommerceStoreMapper;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.util.JwtUtils;
import com.itsworld.pet.dto.PetCreateRequest;
import com.itsworld.pet.service.PetService;
import com.itsworld.user.dto.UserRegisterRequest;
import com.itsworld.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 商业服务集成测试
 * 使用 Testcontainers MySQL，按 init-scripts 初始化完整库结构，覆盖商品履约/退款、预约容量、套餐核销与并发。
 *
 * @author Claude
 * @date 2026-11-07
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommerceFlowIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = createMysql();

    private static MySQLContainer<?> createMysql() {
        MySQLContainer<?> container = new MySQLContainer<>("mysql:8.0")
                .withDatabaseName("its_world")
                .withUsername("itsworld")
                .withPassword("itsworld")
                .withCommand("--character-set-server=utf8mb4",
                        "--collation-server=utf8mb4_unicode_ci")
                .withInitScript("schema-test.sql");
        return container;
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("jwt.secret", () -> "itsworld-integration-test-secret-key-2026-0123456789-abcdefghijklmnopqrstuvwxyz");
        registry.add("minio.endpoint", () -> "http://localhost:9000");
        registry.add("minio.access-key", () -> "test-access");
        registry.add("minio.secret-key", () -> "test-secret");
        registry.add("app.payment.mock-enabled", () -> "true");
    }

    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private PetService petService;
    @Autowired
    private CommerceStoreMapper storeMapper;
    @Autowired
    private ShopCategoryMapper shopCategoryMapper;
    @Autowired
    private ShopProductMapper shopProductMapper;
    @Autowired
    private ShopSkuMapper shopSkuMapper;
    @Autowired
    private ShopInventoryMapper shopInventoryMapper;
    @Autowired
    private GroomingCategoryMapper groomingCategoryMapper;
    @Autowired
    private GroomingServiceMapper groomingServiceMapper;
    @Autowired
    private GroomingServiceStoreMapper groomingServiceStoreMapper;
    @Autowired
    private BookingSlotMapper bookingSlotMapper;
    @Autowired
    private GroomingBookingMapper bookingMapper;
    @Autowired
    private CommerceOrderMapper orderMapper;
    @Autowired
    private AddressService addressService;
    @Autowired
    private CartService cartService;
    @Autowired
    private CheckoutService checkoutService;
    @Autowired
    private CommercePaymentService paymentService;
    @Autowired
    private CommerceOrderService orderService;
    @Autowired
    private BookingService bookingService;
    @Autowired
    private PackageService packageService;
    @Autowired
    private AfterSaleService afterSaleService;
    @Autowired
    private com.itsworld.commerce.fulfillment.service.ShipmentService shipmentService;
    @Autowired
    private ReviewService reviewService;

    @Test
    void contextLoads() {
        assertTrue(MYSQL.isRunning());
    }

    @Test
    void productOrderShipReviewAndRefundFlow() {
        long userId = registerUser("prod");
        long otherUserId = registerUser("prodother");
        CommerceStore store = createStore();
        ShopSku sku = createProduct(store.getId());

        UserShippingAddress address = addressService.createAddress(userId,
                AddressSaveRequest.builder()
                        .recipientName("测试用户")
                        .mobile("13800000000")
                        .province("浙江省")
                        .city("杭州市")
                        .district("西湖区")
                        .addressDetail("文三路 1 号")
                        .isDefault(1)
                        .build());
        cartService.addItem(userId, CartItemAddRequest.builder()
                .skuId(sku.getId())
                .quantity(1)
                .build());
        checkoutService.preview(userId, CheckoutPreviewRequest.builder().build());

        String idempotencyKey = "IT-PROD-" + System.nanoTime();
        OrderSubmitResponse submit = checkoutService.submit(userId,
                CheckoutSubmitRequest.builder()
                        .idempotencyKey(idempotencyKey)
                        .addressId(address.getId())
                        .build());
        OrderSubmitResponse duplicate = checkoutService.submit(userId,
                CheckoutSubmitRequest.builder()
                        .idempotencyKey(idempotencyKey)
                        .addressId(address.getId())
                        .build());
        assertEquals(submit.getOrderNo(), duplicate.getOrderNo());

        paymentService.createPayment(userId, submit.getOrderNo(),
                PaymentCreateRequest.builder().channel("MOCK_WECHAT").build());
        OrderDetailView paid = orderService.getUserOrder(userId, submit.getOrderNo());
        assertEquals("PAID", paid.getOrderStatus());
        Long orderItemId = paid.getItems().get(0).getId();

        assertThrows(BusinessException.class,
                () -> orderService.getUserOrder(otherUserId, submit.getOrderNo()));

        shipmentService.shipOrder(submit.getOrderNo(),
                com.itsworld.commerce.fulfillment.dto.ShipmentCreateRequest.builder()
                        .deliveryCompany("顺丰速运")
                        .trackingNo("SF-IT-" + System.nanoTime())
                        .build(), userId);
        assertEquals("SHIPPED",
                orderService.getUserOrder(userId, submit.getOrderNo()).getOrderStatus());

        shipmentService.confirmReceipt(userId, submit.getOrderNo());
        assertEquals("COMPLETED",
                orderService.getUserOrder(userId, submit.getOrderNo()).getOrderStatus());

        ReviewView review = reviewService.submit(userId, ReviewSubmitRequest.builder()
                .orderNo(submit.getOrderNo())
                .rating(5)
                .content("集成测试评价")
                .build());
        assertEquals("PENDING", review.getStatus());
        ReviewView approved = reviewService.approve(review.getId(), userId);
        assertEquals("APPROVED", approved.getStatus());
        assertEquals("感谢体验", reviewService.reply(review.getId(), "感谢体验").getReply());

        int availableBefore = inventoryOf(sku.getId()).getAvailableQty();
        int soldBefore = inventoryOf(sku.getId()).getSoldQty();
        AfterSaleView afterSale = afterSaleService.apply(userId,
                AfterSaleApplyRequest.builder()
                        .orderNo(submit.getOrderNo())
                        .type("RETURN_REFUND")
                        .reason("集成测试退货")
                        .items(List.of(AfterSaleApplyItemRequest.builder()
                                .orderItemId(orderItemId)
                                .qty(1)
                                .build()))
                        .build());
        assertEquals("APPLIED", afterSale.getStatus());
        afterSaleService.review(afterSale.getAfterSaleNo(),
                AfterSaleReviewRequest.builder().approved(true).remark("同意").build(), userId);
        AfterSaleView refunded = afterSaleService.executeRefund(afterSale.getAfterSaleNo(), userId);
        assertEquals("REFUNDED", refunded.getStatus());
        assertEquals("REFUNDED", refunded.getRefund().getStatus());

        CommerceOrder refundedOrder = orderMapper.selectByOrderNo(submit.getOrderNo());
        assertEquals("REFUNDED", refundedOrder.getPaymentStatus());
        assertEquals(refundedOrder.getPaidAmount(), refundedOrder.getRefundedAmount());
        assertEquals(availableBefore + 1, inventoryOf(sku.getId()).getAvailableQty());
        assertEquals(soldBefore - 1, inventoryOf(sku.getId()).getSoldQty());
    }

    @Test
    void bookingCapacityAndRescheduleFlow() {
        long userId = registerUser("booking");
        long otherUserId = registerUser("bookingother");
        long petId = petService.createPet(userId, PetCreateRequest.builder()
                .petName("测试宠物")
                .petType("dog")
                .weight(new BigDecimal("12.5"))
                .build());
        long otherPetId = petService.createPet(otherUserId, PetCreateRequest.builder()
                .petName("另一只宠物")
                .petType("dog")
                .weight(new BigDecimal("10"))
                .build());
        CommerceStore store = createStore();
        GroomingService service = createGroomingService(store.getId());
        BookingSlot slot = createSlot(store.getId(), LocalTime.of(10, 0), LocalTime.of(11, 0), 1);
        BookingSlot slot2 = createSlot(store.getId(), LocalTime.of(11, 0), LocalTime.of(12, 0), 1);

        BookingCreateResult result = bookingService.createBooking(userId,
                BookingCreateRequest.builder()
                        .storeId(store.getId())
                        .serviceId(service.getId())
                        .petId(petId)
                        .slotId(slot.getId())
                        .payType("ONLINE")
                        .idempotencyKey("IT-BK-" + System.nanoTime())
                        .build());
        assertEquals("PENDING_PAYMENT", result.getStatus());
        assertEquals(1, bookingSlotMapper.selectById(slot.getId()).getLockedCapacity());

        assertThrows(BusinessException.class, () -> bookingService.createBooking(otherUserId,
                BookingCreateRequest.builder()
                        .storeId(store.getId())
                        .serviceId(service.getId())
                        .petId(otherPetId)
                        .slotId(slot.getId())
                        .payType("ONLINE")
                        .idempotencyKey("IT-BK-FULL-" + System.nanoTime())
                        .build()));

        paymentService.createPayment(userId, result.getOrderNo(),
                PaymentCreateRequest.builder().channel("MOCK_WECHAT").build());
        assertEquals("CONFIRMED", bookingService.getUserBooking(userId, result.getBookingNo()).getStatus());
        assertEquals(1, bookingSlotMapper.selectById(slot.getId()).getReservedCapacity());

        bookingService.rescheduleUserBooking(userId, result.getBookingNo(), slot2.getId());
        assertEquals(0, bookingSlotMapper.selectById(slot.getId()).getReservedCapacity());
        assertEquals(1, bookingSlotMapper.selectById(slot2.getId()).getReservedCapacity());

        bookingService.cancelAdminBooking(result.getBookingNo(), "集成测试门店取消", userId);
        assertEquals("CANCELLED", bookingService.getUserBooking(userId, result.getBookingNo()).getStatus());
        assertEquals(0, bookingSlotMapper.selectById(slot2.getId()).getReservedCapacity());
        assertEquals("PAID", orderMapper.selectByOrderNo(result.getOrderNo()).getOrderStatus());

        BookingSlot slot3 = createSlot(store.getId(), LocalTime.of(18, 0), LocalTime.of(19, 0), 1);
        BookingCreateResult pending = bookingService.createBooking(userId,
                BookingCreateRequest.builder()
                        .storeId(store.getId())
                        .serviceId(service.getId())
                        .petId(petId)
                        .slotId(slot3.getId())
                        .payType("ONLINE")
                        .idempotencyKey("IT-BK-CANCEL-" + System.nanoTime())
                        .build());
        bookingService.cancelUserBooking(userId, pending.getBookingNo(), "集成测试用户取消");
        assertEquals("CANCELLED", bookingService.getUserBooking(userId, pending.getBookingNo()).getStatus());
        assertEquals(0, bookingSlotMapper.selectById(slot3.getId()).getLockedCapacity());
        assertEquals("CANCELLED", orderMapper.selectByOrderNo(pending.getOrderNo()).getOrderStatus());
    }

    @Test
    void packagePurchaseAndUsageFlow() {
        long userId = registerUser("package");
        long petId = petService.createPet(userId, PetCreateRequest.builder()
                .petName("套餐宠物")
                .petType("dog")
                .weight(new BigDecimal("8"))
                .build());
        CommerceStore store = createStore();
        GroomingService service = createGroomingService(store.getId());

        PackageTemplateView template = packageService.createTemplate(
                PackageTemplateSaveRequest.builder()
                        .packageCode("IT-PKG-" + System.nanoTime())
                        .name("集成测试套餐")
                        .salePrice(10000L)
                        .validDays(30)
                        .status("ENABLED")
                        .allowCrossStore(0)
                        .items(List.of(PackageItemRequest.builder()
                                .serviceId(service.getId())
                                .includedTimes(2)
                                .build()))
                        .options(List.of())
                        .build());
        assertNotNull(template.getId());

        PackagePurchaseResult purchase = packageService.purchasePackage(userId,
                PackagePurchaseRequest.builder()
                        .templateId(template.getId())
                        .petId(petId)
                        .storeId(store.getId())
                        .optionItemIds(List.of())
                        .idempotencyKey("IT-PKG-ORDER-" + System.nanoTime())
                        .build());
        paymentService.createPayment(userId, purchase.getOrderNo(),
                PaymentCreateRequest.builder().channel("MOCK_WECHAT").build());

        List<UserPackageView> packages = packageService.pageMyPackages(userId, "ACTIVE", 1, 10)
                .getRecords();
        UserPackageView mine = packages.stream()
                .filter(item -> item.getPackageId().equals(template.getId()))
                .findFirst().orElseThrow();
        assertEquals(2, mine.getItems().get(0).getTotalTimes());

        BookingSlot slot = createSlot(store.getId(), LocalTime.of(14, 0), LocalTime.of(15, 0), 1);
        BookingCreateResult booking = bookingService.createBooking(userId,
                BookingCreateRequest.builder()
                        .storeId(store.getId())
                        .serviceId(service.getId())
                        .petId(petId)
                        .slotId(slot.getId())
                        .payType("PACKAGE")
                        .userPackageItemId(mine.getItems().get(0).getId())
                        .idempotencyKey("IT-PKG-BK-" + System.nanoTime())
                        .build());
        assertEquals("CONFIRMED", booking.getStatus());
        Long bookingId = bookingMapper.selectByBookingNo(booking.getBookingNo()).getId();
        bookingService.checkinBooking(booking.getBookingNo(), userId);
        bookingService.startBooking(booking.getBookingNo(), userId);
        bookingService.completeBooking(booking.getBookingNo(), userId);

        UserPackageView used = packageService.getMyPackage(userId, mine.getId());
        assertEquals(1, used.getItems().get(0).getUsedTimes());
        assertEquals(1, used.getItems().get(0).getRemainingTimes());
        assertEquals(1, packageService.listUsages(userId, mine.getId()).size());
        assertTrue(packageService.listUsages(userId, mine.getId()).get(0).getStatus().equals("USED"));
        assertNotNull(bookingId);
    }

    @Test
    void slotCapacityIsAtomic() throws Exception {
        CommerceStore store = createStore();
        BookingSlot slot = createSlot(store.getId(), LocalTime.of(16, 0), LocalTime.of(17, 0), 1);
        AtomicInteger slotSuccess = new AtomicInteger();
        runConcurrently(() -> {
            if (bookingSlotMapper.lockCapacity(slot.getId(), 1) == 1) {
                slotSuccess.incrementAndGet();
            }
        });
        assertEquals(1, slotSuccess.get());
    }

    private void runConcurrently(Runnable task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicLong failures = new AtomicLong();
        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await(5, TimeUnit.SECONDS);
                    task.run();
                } catch (Throwable e) {
                    failures.incrementAndGet();
                }
            });
        }
        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        assertEquals(0, failures.get());
    }

    private static final java.util.concurrent.atomic.AtomicLong USER_SEQ =
            new java.util.concurrent.atomic.AtomicLong(1);


    @Autowired
    private com.itsworld.member.service.MemberCenterService memberCenter;
    @Autowired
    private com.itsworld.member.service.MemberService memberService;
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate memberJdbc;

    @Test
    void paidMembershipRenewalExpiryAndGrowthRedemption() {
        long user = registerUser("member");
        long other = registerUser("memberother");
        Long level = memberJdbc.queryForObject("SELECT id FROM member_level WHERE discount_rate<1 ORDER BY id LIMIT 1",Long.class);
        String planName = "测试月卡" + System.nanoTime();
        memberCenter.savePlan(null,new com.itsworld.member.dto.MemberCenterRequests.Plan(planName,level,100L,30,1));
        Long plan = memberJdbc.queryForObject("SELECT id FROM member_subscription_plan WHERE name=?",Long.class,planName);
        var request = new com.itsworld.member.dto.MemberCenterRequests.Purchase(plan,"first");
        String no = memberCenter.purchase(user,request);
        assertEquals(no,memberCenter.purchase(user,request));
        assertEquals("NOT_OPEN",memberService.getOverview(user).getMembershipStatus());
        PaymentCreateRequest pay = new PaymentCreateRequest();
        pay.setChannel("MOCK_WECHAT");
        assertThrows(BusinessException.class,()->paymentService.createPayment(other,no,pay));
        paymentService.createPayment(user,no,pay);
        assertEquals("ACTIVE",memberService.getOverview(user).getMembershipStatus());
        LocalDateTime firstEnd = memberService.getOverview(user).getMemberExpiresAt();
        assertTrue(memberService.getCurrentDiscountRate(user).compareTo(BigDecimal.ONE)<0);
        paymentService.createPayment(user,no,pay);
        assertEquals(firstEnd,memberService.getOverview(user).getMemberExpiresAt());
        String renewal=memberCenter.purchase(user,new com.itsworld.member.dto.MemberCenterRequests.Purchase(plan,"renew"));
        paymentService.createPayment(user,renewal,pay);
        assertEquals(firstEnd.plusDays(30),memberService.getOverview(user).getMemberExpiresAt());
        memberService.award(user,1000,"TEST","growth:"+user,"测试奖励");
        assertEquals(level,memberService.getOrCreateAccount(user).getLevelId());
        String rewardName="测试礼品"+System.nanoTime();
        memberCenter.saveReward(null,new com.itsworld.member.dto.MemberCenterRequests.Reward(rewardName,100,1,1));
        Long reward=memberJdbc.queryForObject("SELECT id FROM member_reward_product WHERE name=?",Long.class,rewardName);
        var redeem=new com.itsworld.member.dto.MemberCenterRequests.Redeem(reward,"redeem");
        String redemption=memberCenter.redeem(user,redeem);
        assertEquals(redemption,memberCenter.redeem(user,redeem));
        assertEquals(900,memberService.getOrCreateAccount(user).getGrowthValue());
        assertThrows(BusinessException.class,()->memberCenter.redeem(user,new com.itsworld.member.dto.MemberCenterRequests.Redeem(reward,"soldout")));
        memberCenter.fulfill(redemption,user);
        memberCenter.fulfill(redemption,user);
        memberCenter.saveReward(reward,new com.itsworld.member.dto.MemberCenterRequests.Reward(rewardName,2000,1,1));
        assertThrows(BusinessException.class,()->memberCenter.redeem(user,new com.itsworld.member.dto.MemberCenterRequests.Redeem(reward,"insufficient")));
        assertEquals(1,memberJdbc.queryForObject("SELECT stock FROM member_reward_product WHERE id=?",Integer.class,reward));
        memberJdbc.update("UPDATE member_account SET member_expires_at=DATE_SUB(NOW(),INTERVAL 1 DAY) WHERE user_id=?",user);
        assertEquals("EXPIRED",memberService.getOverview(user).getMembershipStatus());
        assertEquals(BigDecimal.ONE,memberService.getCurrentDiscountRate(user));
        assertEquals(900,memberService.getOrCreateAccount(user).getGrowthValue());
    }

    @Test
    void rewardStockCannotBeOversoldByConcurrentUsers() throws Exception {
        long first=registerUser("rewardfirst");
        long second=registerUser("rewardsecond");
        memberService.award(first,500,"TEST","seed:"+first,"测试成长值");
        memberService.award(second,500,"TEST","seed:"+second,"测试成长值");
        String name="限量礼品"+System.nanoTime();
        memberCenter.saveReward(null,new com.itsworld.member.dto.MemberCenterRequests.Reward(name,100,1,1));
        Long product=memberJdbc.queryForObject("SELECT id FROM member_reward_product WHERE name=?",Long.class,name);
        ExecutorService pool=Executors.newFixedThreadPool(2);
        CountDownLatch start=new CountDownLatch(1);
        AtomicInteger successes=new AtomicInteger();
        try {
            var one=pool.submit(()-> {
                try { start.await(); memberCenter.redeem(first,new com.itsworld.member.dto.MemberCenterRequests.Redeem(product,"one")); successes.incrementAndGet(); }
                catch(BusinessException expected) { /* Competing user consumed stock. */ }
                catch(InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException(interrupted); }
            });
            var two=pool.submit(()-> {
                try { start.await(); memberCenter.redeem(second,new com.itsworld.member.dto.MemberCenterRequests.Redeem(product,"two")); successes.incrementAndGet(); }
                catch(BusinessException expected) { /* Competing user consumed stock. */ }
                catch(InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException(interrupted); }
            });
            start.countDown();
            one.get(15,TimeUnit.SECONDS);
            two.get(15,TimeUnit.SECONDS);
            assertEquals(1,successes.get());
            assertEquals(0,memberJdbc.queryForObject("SELECT stock FROM member_reward_product WHERE id=?",Integer.class,product));
            assertEquals(900,memberService.getOrCreateAccount(first).getGrowthValue()+memberService.getOrCreateAccount(second).getGrowthValue());
        } finally { pool.shutdownNow(); }
    }

    @Autowired
    private org.springframework.test.web.servlet.MockMvc memberMvc;

    @Test
    void memberApisEnforceRolesValidateRequestsAndHidePoints() throws Exception {
        long user=registerUser("memberapi");
        String username=memberJdbc.queryForObject("SELECT username FROM sys_user WHERE id=?",String.class,user);
        String token=jwtUtils.generateToken(String.valueOf(user),username);
        memberMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/member/overview")
                .header("Authorization","Bearer "+token))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.membershipStatus").value("NOT_OPEN"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.pointsBalance").doesNotExist());
        memberMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/member/subscription-plans")
                .header("Authorization","Bearer "+token))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        memberMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/member/subscription-orders")
                .header("Authorization","Bearer "+token).contentType("application/json").content("{}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(org.hamcrest.Matchers.not(0)));
    }

    private long registerUser(String prefix) {
        long suffix = USER_SEQ.getAndIncrement();
        String username = prefix + System.nanoTime() + suffix;
        String phone = "13" + String.format("%09d", suffix);
        Long userId = userService.register(UserRegisterRequest.builder()
                .username(username)
                .phone(phone)
                .password("Test12345")
                .confirmPassword("Test12345")
                .build());
        assertNotNull(jwtUtils.generateToken(String.valueOf(userId), username));
        return userId;
    }

    private CommerceStore createStore() {
        long suffix = System.nanoTime();
        CommerceStore store = CommerceStore.builder()
                .storeCode("IT-STORE-" + suffix)
                .name("集成测试门店")
                .businessStatus("OPEN")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        storeMapper.insert(store);
        return store;
    }

    private ShopSku createProduct(Long storeId) {
        long suffix = System.nanoTime();
        ShopCategory category = ShopCategory.builder()
                .name("集成测试分类")
                .sort(0)
                .status("ENABLED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        shopCategoryMapper.insert(category);
        ShopProduct product = ShopProduct.builder()
                .productCode("IT-PRODUCT-" + suffix)
                .categoryId(category.getId())
                .name("集成测试商品")
                .saleStatus("ON_SALE")
                .sort(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        shopProductMapper.insert(product);
        ShopSku sku = ShopSku.builder()
                .productId(product.getId())
                .skuCode("IT-SKU-" + suffix)
                .salePrice(3990L)
                .marketPrice(4990L)
                .status("ENABLED")
                .version(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        shopSkuMapper.insert(sku);
        shopInventoryMapper.insert(ShopInventory.builder()
                .skuId(sku.getId())
                .warehouseId(0L)
                .availableQty(10)
                .lockedQty(0)
                .soldQty(0)
                .version(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
        return sku;
    }

    private GroomingService createGroomingService(Long storeId) {
        long suffix = System.nanoTime();
        GroomingCategory category = GroomingCategory.builder()
                .name("集成测试美容分类")
                .sort(0)
                .status("ENABLED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        groomingCategoryMapper.insert(category);
        GroomingService service = GroomingService.builder()
                .categoryId(category.getId())
                .serviceCode("IT-BATH-" + suffix)
                .name("集成测试洗护")
                .durationMin(60)
                .basePrice(10000L)
                .status("ENABLED")
                .sort(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        groomingServiceMapper.insert(service);
        groomingServiceStoreMapper.insert(GroomingServiceStore.builder()
                .serviceId(service.getId())
                .storeId(storeId)
                .salePrice(9000L)
                .capacityPerSlot(1)
                .status("ENABLED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
        return service;
    }

    private BookingSlot createSlot(Long storeId, LocalTime start, LocalTime end, int capacity) {
        BookingSlot slot = BookingSlot.builder()
                .storeId(storeId)
                .staffId(0L)
                .serviceDate(LocalDate.now().plusDays(1))
                .startTime(start)
                .endTime(end)
                .totalCapacity(capacity)
                .reservedCapacity(0)
                .lockedCapacity(0)
                .status("OPEN")
                .version(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        bookingSlotMapper.insert(slot);
        return slot;
    }

    private ShopInventory inventoryOf(Long skuId) {
        return shopInventoryMapper.selectBySkuIdAndWarehouse(skuId, 0L);
    }

}
