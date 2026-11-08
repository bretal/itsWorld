package com.itsworld.commerce.order.job;

import com.itsworld.commerce.order.service.CommerceOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单支付超时关闭任务
 *
 * @author Claude
 * @date 2026-11-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpireJob {

    private final CommerceOrderService orderService;

    /**
     * 每分钟扫描一次超时未支付订单；条件更新 + 库存/优惠券释放保证可重复执行。
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    public void closeExpiredOrders() {
        try {
            orderService.closeExpiredOrders();
        } catch (Exception e) {
            log.error("关闭超时订单任务失败: {}", e.getMessage(), e);
        }
    }

}
