package com.itsworld.commerce.grooming.service;

import com.itsworld.commerce.grooming.dto.GroomingCategorySaveRequest;
import com.itsworld.commerce.grooming.dto.GroomingPetRuleSaveRequest;
import com.itsworld.commerce.grooming.dto.GroomingServiceDetailView;
import com.itsworld.commerce.grooming.dto.GroomingServiceSaveRequest;
import com.itsworld.commerce.grooming.dto.GroomingServiceStoreSaveRequest;
import com.itsworld.commerce.grooming.dto.GroomingServiceView;
import com.itsworld.commerce.grooming.entity.GroomingCategory;
import com.itsworld.commerce.grooming.entity.GroomingService;
import com.itsworld.commerce.grooming.entity.GroomingServicePetRule;
import com.itsworld.commerce.grooming.entity.GroomingServiceStore;
import com.itsworld.common.result.PageResult;

import java.util.List;

/**
 * 美容服务业务接口
 * 命名避开实体 GroomingService 的类名冲突。
 *
 * @author Claude
 * @date 2026-11-07
 */
public interface GroomingCatalogService {

    List<GroomingCategory> listEnabledCategories();

    PageResult<GroomingServiceView> pageServices(Long storeId, Long categoryId, String keyword,
                                                 long pageNum, long pageSize, Long userId);

    GroomingServiceDetailView getServiceDetail(Long serviceId, Long storeId, Long petId, Long userId);

    /**
     * 获取启用的美容服务，不存在或已下架时抛业务异常
     */
    GroomingService requireEnabledService(Long serviceId);

    /**
     * 获取门店启用的服务配置，未配置/已停用时抛业务异常
     */
    GroomingServiceStore requireEnabledServiceStore(Long serviceId, Long storeId);

    /**
     * 解析门店服务价：优先门店价，否则基础价
     */
    long resolveStorePrice(Long serviceId, Long storeId);

    /**
     * 校验宠物是否适配指定服务，不适配时抛业务异常
     */
    void validatePetEligible(Long serviceId, Long userId, Long petId);

    List<GroomingCategory> listAdminCategories();

    GroomingCategory createCategory(GroomingCategorySaveRequest request);

    GroomingCategory updateCategory(Long categoryId, GroomingCategorySaveRequest request);

    void changeCategoryStatus(Long categoryId, String status);

    PageResult<GroomingService> pageAdminServices(Long categoryId, String keyword,
                                                  long pageNum, long pageSize);

    GroomingService getAdminService(Long serviceId);

    GroomingService createService(GroomingServiceSaveRequest request);

    GroomingService updateService(Long serviceId, GroomingServiceSaveRequest request);

    void changeServiceStatus(Long serviceId, String status);

    List<GroomingServiceStore> listServiceStores(Long serviceId);

    GroomingServiceStore createServiceStore(Long serviceId, GroomingServiceStoreSaveRequest request);

    GroomingServiceStore updateServiceStore(Long id, GroomingServiceStoreSaveRequest request);

    void deleteServiceStore(Long id);

    List<GroomingServicePetRule> listPetRules(Long serviceId);

    GroomingServicePetRule createPetRule(Long serviceId, GroomingPetRuleSaveRequest request);

    GroomingServicePetRule updatePetRule(Long ruleId, GroomingPetRuleSaveRequest request);

    void deletePetRule(Long ruleId);

}
