package com.itsworld.commerce.store.controller;

import com.itsworld.commerce.store.dto.StoreDetailView;
import com.itsworld.commerce.store.dto.StoreView;
import com.itsworld.commerce.store.service.StoreService;
import com.itsworld.common.result.PageResult;
import com.itsworld.common.result.Result;
import com.itsworld.common.util.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端门店控制器
 *
 * @author Claude
 * @date 2026-11-07
 */
@RestController
@RequestMapping("/api/commerce/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    /**
     * 门店列表（仅营业中）
     */
    @GetMapping
    public Result<PageResult<StoreView>> listStores(
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size,
            @RequestParam(required = false) String keyword) {
        return Result.success(storeService.pageStores(
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size), keyword));
    }

    /**
     * 门店详情
     */
    @GetMapping("/{storeId}")
    public Result<StoreDetailView> getStoreDetail(@PathVariable Long storeId) {
        return Result.success(storeService.getStoreDetail(storeId));
    }

}
