package com.itsworld.commerce.fulfillment.job;

import com.itsworld.commerce.fulfillment.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 发货 7 天自动确认收货任务
 *
 * @author Claude
 * @date 2026-11-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShipmentAutoConfirmJob {

    private final ShipmentService shipmentService;

    /**
     * 每小时扫描一次已发货超过 7 天的发货单
     */
    @Scheduled(fixedDelay = 3600000, initialDelay = 60000)
    public void autoConfirmReceipts() {
        try {
            shipmentService.autoConfirmReceipts();
        } catch (Exception e) {
            log.error("自动确认收货任务失败: {}", e.getMessage(), e);
        }
    }

}
