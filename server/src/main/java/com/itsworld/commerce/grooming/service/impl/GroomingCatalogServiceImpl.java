package com.itsworld.commerce.grooming.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itsworld.commerce.common.CommercePriceUtils;
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
import com.itsworld.commerce.grooming.mapper.GroomingCategoryMapper;
import com.itsworld.commerce.grooming.mapper.GroomingServiceMapper;
import com.itsworld.commerce.grooming.mapper.GroomingServicePetRuleMapper;
import com.itsworld.commerce.grooming.mapper.GroomingServiceStoreMapper;
import com.itsworld.commerce.grooming.service.GroomingCatalogService;
import com.itsworld.commerce.store.mapper.CommerceStoreMapper;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.ErrorCode;
import com.itsworld.common.result.PageResult;
import com.itsworld.member.service.MemberService;
import com.itsworld.pet.dto.PetResponse;
import com.itsworld.pet.service.PetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 美容服务业务实现
 *
 * @author Claude
 * @date 2026-11-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroomingCatalogServiceImpl implements GroomingCatalogService {

    private static final String ENABLED = "ENABLED";
    private static final String DISABLED = "DISABLED";

    private final GroomingCategoryMapper categoryMapper;
    private final GroomingServiceMapper serviceMapper;
    private final GroomingServiceStoreMapper serviceStoreMapper;
    private final GroomingServicePetRuleMapper petRuleMapper;
    private final CommerceStoreMapper storeMapper;
    private final MemberService memberService;
    private final PetService petService;

    @Override
    public List<GroomingCategory> listEnabledCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<GroomingCategory>()
                .eq(GroomingCategory::getStatus, ENABLED)
                .orderByAsc(GroomingCategory::getSort)
                .orderByAsc(GroomingCategory::getId));
    }

    @Override
    public PageResult<GroomingServiceView> pageServices(Long storeId, Long categoryId, String keyword,
                                                        long pageNum, long pageSize, Long userId) {
        Page<GroomingService> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<GroomingService> wrapper = new LambdaQueryWrapper<GroomingService>()
                .eq(GroomingService::getStatus, ENABLED)
                .orderByAsc(GroomingService::getSort)
                .orderByAsc(GroomingService::getId);
        if (categoryId != null) {
            wrapper.eq(GroomingService::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(w -> w.like(GroomingService::getName, value)
                    .or().like(GroomingService::getServiceCode, value));
        }
        if (storeId != null) {
            List<GroomingServiceStore> serviceStores = serviceStoreMapper.selectList(
                    new LambdaQueryWrapper<GroomingServiceStore>()
                            .eq(GroomingServiceStore::getStoreId, storeId)
                            .eq(GroomingServiceStore::getStatus, ENABLED));
            if (serviceStores.isEmpty()) {
                return PageResult.of(List.of(), 0L, pageNum, pageSize);
            }
            wrapper.in(GroomingService::getId,
                    serviceStores.stream().map(GroomingServiceStore::getServiceId).toList());
        }

        Page<GroomingService> result = serviceMapper.selectPage(page, wrapper);
        BigDecimal discountRate = memberService.getCurrentDiscountRate(userId);
        Map<Long, String> categoryNames = categoryNameMap();
        List<GroomingServiceView> records = new ArrayList<>();
        for (GroomingService service : result.getRecords()) {
            long storePrice = resolveStorePrice(service, storeId);
            records.add(GroomingServiceView.builder()
                    .id(service.getId())
                    .categoryId(service.getCategoryId())
                    .categoryName(categoryNames.get(service.getCategoryId()))
                    .serviceCode(service.getServiceCode())
                    .name(service.getName())
                    .coverUrl(service.getCoverUrl())
                    .description(service.getDescription())
                    .durationMin(service.getDurationMin())
                    .basePrice(service.getBasePrice())
                    .storePrice(storePrice)
                    .memberPrice(CommercePriceUtils.applyDiscount(storePrice, discountRate))
                    .status(service.getStatus())
                    .sort(service.getSort())
                    .build());
        }
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public GroomingServiceDetailView getServiceDetail(Long serviceId, Long storeId, Long petId, Long userId) {
        GroomingService service = requireService(serviceId);
        if (!ENABLED.equals(service.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "服务已下架");
        }
        List<GroomingServicePetRule> rules = listPetRules(serviceId);
        BigDecimal discountRate = memberService.getCurrentDiscountRate(userId);
        long storePrice = resolveStorePrice(service, storeId);

        Boolean petEligible = null;
        String ineligibleReason = null;
        if (petId != null) {
            PetResponse pet = petService.getPetInfo(userId, petId);
            Eligibility eligibility = evaluatePetEligibility(rules, pet);
            petEligible = eligibility.eligible();
            ineligibleReason = eligibility.reason();
        }

        return GroomingServiceDetailView.builder()
                .id(service.getId())
                .categoryId(service.getCategoryId())
                .categoryName(categoryNameMap().get(service.getCategoryId()))
                .serviceCode(service.getServiceCode())
                .name(service.getName())
                .coverUrl(service.getCoverUrl())
                .description(service.getDescription())
                .durationMin(service.getDurationMin())
                .basePrice(service.getBasePrice())
                .storePrice(storePrice)
                .memberPrice(CommercePriceUtils.applyDiscount(storePrice, discountRate))
                .status(service.getStatus())
                .sort(service.getSort())
                .petRules(rules)
                .petEligible(petEligible)
                .ineligibleReason(ineligibleReason)
                .build();
    }

    @Override
    public GroomingService requireEnabledService(Long serviceId) {
        GroomingService service = requireService(serviceId);
        if (!ENABLED.equals(service.getStatus())) {
            throw new BusinessException(ErrorCode.COMMERCE_SERVICE_NOT_AVAILABLE);
        }
        return service;
    }

    @Override
    public GroomingServiceStore requireEnabledServiceStore(Long serviceId, Long storeId) {
        GroomingServiceStore serviceStore = serviceStoreMapper.selectOne(
                new LambdaQueryWrapper<GroomingServiceStore>()
                        .eq(GroomingServiceStore::getServiceId, serviceId)
                        .eq(GroomingServiceStore::getStoreId, storeId)
                        .eq(GroomingServiceStore::getStatus, ENABLED)
                        .last("LIMIT 1"));
        if (serviceStore == null) {
            throw new BusinessException(ErrorCode.COMMERCE_SERVICE_NOT_AVAILABLE);
        }
        return serviceStore;
    }

    @Override
    public long resolveStorePrice(Long serviceId, Long storeId) {
        return resolveStorePrice(requireService(serviceId), storeId);
    }

    @Override
    public void validatePetEligible(Long serviceId, Long userId, Long petId) {
        PetResponse pet = petService.getPetInfo(userId, petId);
        Eligibility eligibility = evaluatePetEligibility(listPetRules(serviceId), pet);
        if (!eligibility.eligible()) {
            throw new BusinessException(ErrorCode.BOOKING_PET_NOT_ELIGIBLE.getCode(),
                    eligibility.reason() != null ? eligibility.reason()
                            : ErrorCode.BOOKING_PET_NOT_ELIGIBLE.getMessage());
        }
    }

    @Override
    public List<GroomingCategory> listAdminCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<GroomingCategory>()
                .orderByAsc(GroomingCategory::getSort)
                .orderByAsc(GroomingCategory::getId));
    }

    @Override
    @Transactional
    public GroomingCategory createCategory(GroomingCategorySaveRequest request) {
        GroomingCategory category = GroomingCategory.builder()
                .name(request.getName().trim())
                .iconUrl(request.getIconUrl())
                .sort(request.getSort() != null ? request.getSort() : 0)
                .status(normalizeStatus(request.getStatus()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        categoryMapper.insert(category);
        return category;
    }

    @Override
    @Transactional
    public GroomingCategory updateCategory(Long categoryId, GroomingCategorySaveRequest request) {
        GroomingCategory category = requireCategory(categoryId);
        category.setName(request.getName().trim());
        category.setIconUrl(request.getIconUrl());
        category.setSort(request.getSort() != null ? request.getSort() : category.getSort());
        category.setStatus(normalizeStatus(request.getStatus()));
        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.updateById(category);
        return category;
    }

    @Override
    @Transactional
    public void changeCategoryStatus(Long categoryId, String status) {
        GroomingCategory category = requireCategory(categoryId);
        category.setStatus(normalizeStatus(status));
        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.updateById(category);
    }

    @Override
    public PageResult<GroomingService> pageAdminServices(Long categoryId, String keyword,
                                                         long pageNum, long pageSize) {
        Page<GroomingService> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<GroomingService> wrapper = new LambdaQueryWrapper<GroomingService>()
                .orderByAsc(GroomingService::getSort)
                .orderByAsc(GroomingService::getId);
        if (categoryId != null) {
            wrapper.eq(GroomingService::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(w -> w.like(GroomingService::getName, value)
                    .or().like(GroomingService::getServiceCode, value));
        }
        Page<GroomingService> result = serviceMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public GroomingService getAdminService(Long serviceId) {
        return requireService(serviceId);
    }

    @Override
    @Transactional
    public GroomingService createService(GroomingServiceSaveRequest request) {
        validateCategory(request.getCategoryId());
        validateServiceCode(request.getServiceCode(), null);
        GroomingService service = GroomingService.builder()
                .categoryId(request.getCategoryId())
                .serviceCode(request.getServiceCode().trim())
                .name(request.getName().trim())
                .coverUrl(request.getCoverUrl())
                .description(request.getDescription())
                .durationMin(request.getDurationMin())
                .basePrice(request.getBasePrice())
                .status(normalizeStatus(request.getStatus()))
                .sort(request.getSort() != null ? request.getSort() : 0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        serviceMapper.insert(service);
        return service;
    }

    @Override
    @Transactional
    public GroomingService updateService(Long serviceId, GroomingServiceSaveRequest request) {
        GroomingService service = requireService(serviceId);
        validateCategory(request.getCategoryId());
        validateServiceCode(request.getServiceCode(), serviceId);
        service.setCategoryId(request.getCategoryId());
        service.setServiceCode(request.getServiceCode().trim());
        service.setName(request.getName().trim());
        service.setCoverUrl(request.getCoverUrl());
        service.setDescription(request.getDescription());
        service.setDurationMin(request.getDurationMin());
        service.setBasePrice(request.getBasePrice());
        service.setStatus(normalizeStatus(request.getStatus()));
        service.setSort(request.getSort() != null ? request.getSort() : service.getSort());
        service.setUpdatedAt(LocalDateTime.now());
        serviceMapper.updateById(service);
        return service;
    }

    @Override
    @Transactional
    public void changeServiceStatus(Long serviceId, String status) {
        GroomingService service = requireService(serviceId);
        service.setStatus(normalizeStatus(status));
        service.setUpdatedAt(LocalDateTime.now());
        serviceMapper.updateById(service);
    }

    @Override
    public List<GroomingServiceStore> listServiceStores(Long serviceId) {
        requireService(serviceId);
        return serviceStoreMapper.selectList(new LambdaQueryWrapper<GroomingServiceStore>()
                .eq(GroomingServiceStore::getServiceId, serviceId)
                .orderByAsc(GroomingServiceStore::getId));
    }

    @Override
    @Transactional
    public GroomingServiceStore createServiceStore(Long serviceId,
                                                   GroomingServiceStoreSaveRequest request) {
        requireService(serviceId);
        requireStore(request.getStoreId());
        Long count = serviceStoreMapper.selectCount(new LambdaQueryWrapper<GroomingServiceStore>()
                .eq(GroomingServiceStore::getServiceId, serviceId)
                .eq(GroomingServiceStore::getStoreId, request.getStoreId()));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "该门店已配置此服务");
        }
        GroomingServiceStore serviceStore = GroomingServiceStore.builder()
                .serviceId(serviceId)
                .storeId(request.getStoreId())
                .salePrice(request.getSalePrice())
                .capacityPerSlot(request.getCapacityPerSlot())
                .status(normalizeStatus(request.getStatus()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        serviceStoreMapper.insert(serviceStore);
        return serviceStore;
    }

    @Override
    @Transactional
    public GroomingServiceStore updateServiceStore(Long id, GroomingServiceStoreSaveRequest request) {
        GroomingServiceStore serviceStore = serviceStoreMapper.selectById(id);
        if (serviceStore == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "门店服务配置不存在");
        }
        requireStore(request.getStoreId());
        Long count = serviceStoreMapper.selectCount(new LambdaQueryWrapper<GroomingServiceStore>()
                .eq(GroomingServiceStore::getServiceId, serviceStore.getServiceId())
                .eq(GroomingServiceStore::getStoreId, request.getStoreId())
                .ne(GroomingServiceStore::getId, id));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "该门店已配置此服务");
        }
        serviceStore.setStoreId(request.getStoreId());
        serviceStore.setSalePrice(request.getSalePrice());
        serviceStore.setCapacityPerSlot(request.getCapacityPerSlot());
        serviceStore.setStatus(normalizeStatus(request.getStatus()));
        serviceStore.setUpdatedAt(LocalDateTime.now());
        serviceStoreMapper.updateById(serviceStore);
        return serviceStore;
    }

    @Override
    @Transactional
    public void deleteServiceStore(Long id) {
        serviceStoreMapper.deleteById(id);
    }

    @Override
    public List<GroomingServicePetRule> listPetRules(Long serviceId) {
        return petRuleMapper.selectList(new LambdaQueryWrapper<GroomingServicePetRule>()
                .eq(GroomingServicePetRule::getServiceId, serviceId)
                .orderByAsc(GroomingServicePetRule::getId));
    }

    @Override
    @Transactional
    public GroomingServicePetRule createPetRule(Long serviceId, GroomingPetRuleSaveRequest request) {
        requireService(serviceId);
        GroomingServicePetRule rule = GroomingServicePetRule.builder()
                .serviceId(serviceId)
                .speciesCode(request.getSpeciesCode().trim())
                .minWeightGram(request.getMinWeightGram())
                .maxWeightGram(request.getMaxWeightGram())
                .minAgeMonth(request.getMinAgeMonth())
                .maxAgeMonth(request.getMaxAgeMonth())
                .note(request.getNote())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        petRuleMapper.insert(rule);
        return rule;
    }

    @Override
    @Transactional
    public GroomingServicePetRule updatePetRule(Long ruleId, GroomingPetRuleSaveRequest request) {
        GroomingServicePetRule rule = petRuleMapper.selectById(ruleId);
        if (rule == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "宠物适配规则不存在");
        }
        rule.setSpeciesCode(request.getSpeciesCode().trim());
        rule.setMinWeightGram(request.getMinWeightGram());
        rule.setMaxWeightGram(request.getMaxWeightGram());
        rule.setMinAgeMonth(request.getMinAgeMonth());
        rule.setMaxAgeMonth(request.getMaxAgeMonth());
        rule.setNote(request.getNote());
        rule.setUpdatedAt(LocalDateTime.now());
        petRuleMapper.updateById(rule);
        return rule;
    }

    @Override
    @Transactional
    public void deletePetRule(Long ruleId) {
        petRuleMapper.deleteById(ruleId);
    }

    private long resolveStorePrice(GroomingService service, Long storeId) {
        if (storeId == null) {
            return service.getBasePrice() != null ? service.getBasePrice() : 0L;
        }
        GroomingServiceStore serviceStore = serviceStoreMapper.selectOne(
                new LambdaQueryWrapper<GroomingServiceStore>()
                        .eq(GroomingServiceStore::getServiceId, service.getId())
                        .eq(GroomingServiceStore::getStoreId, storeId)
                        .eq(GroomingServiceStore::getStatus, ENABLED)
                        .last("LIMIT 1"));
        if (serviceStore != null && serviceStore.getSalePrice() != null) {
            return serviceStore.getSalePrice();
        }
        return service.getBasePrice() != null ? service.getBasePrice() : 0L;
    }

    private Eligibility evaluatePetEligibility(List<GroomingServicePetRule> rules, PetResponse pet) {
        if (rules.isEmpty()) {
            return new Eligibility(true, null);
        }
        List<GroomingServicePetRule> speciesRules = rules.stream()
                .filter(rule -> Objects.equals(rule.getSpeciesCode(), pet.getPetType()))
                .toList();
        if (speciesRules.isEmpty()) {
            return new Eligibility(true, null);
        }
        for (GroomingServicePetRule rule : speciesRules) {
            if (matchesRule(rule, pet)) {
                return new Eligibility(true, null);
            }
        }
        String reason = speciesRules.stream()
                .map(GroomingServicePetRule::getNote)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("宠物不符合该服务的适配条件");
        return new Eligibility(false, reason);
    }

    private boolean matchesRule(GroomingServicePetRule rule, PetResponse pet) {
        if (pet.getWeight() != null) {
            int weightGram = pet.getWeight().multiply(BigDecimal.valueOf(1000)).intValue();
            if (rule.getMinWeightGram() != null && weightGram < rule.getMinWeightGram()) {
                return false;
            }
            if (rule.getMaxWeightGram() != null && weightGram > rule.getMaxWeightGram()) {
                return false;
            }
        }
        if (StringUtils.hasText(pet.getBirthDate())) {
            try {
                long ageMonth = Period.between(LocalDate.parse(pet.getBirthDate()), LocalDate.now())
                        .toTotalMonths();
                if (rule.getMinAgeMonth() != null && ageMonth < rule.getMinAgeMonth()) {
                    return false;
                }
                if (rule.getMaxAgeMonth() != null && ageMonth > rule.getMaxAgeMonth()) {
                    return false;
                }
            } catch (DateTimeParseException ignored) {
                log.debug("宠物出生日期解析失败: petId={}, birthDate={}", pet.getId(), pet.getBirthDate());
            }
        }
        return true;
    }

    private Map<Long, String> categoryNameMap() {
        Map<Long, String> map = new HashMap<>();
        for (GroomingCategory category : categoryMapper.selectList(null)) {
            map.put(category.getId(), category.getName());
        }
        return map;
    }

    private GroomingCategory requireCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        GroomingCategory category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "美容分类不存在");
        }
        return category;
    }

    private void validateCategory(Long categoryId) {
        if (categoryId != null) {
            requireCategory(categoryId);
        }
    }

    private GroomingService requireService(Long serviceId) {
        GroomingService service = serviceMapper.selectById(serviceId);
        if (service == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "美容服务不存在");
        }
        return service;
    }

    private void requireStore(Long storeId) {
        if (storeMapper.selectById(storeId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "门店不存在");
        }
    }

    private void validateServiceCode(String serviceCode, Long excludeId) {
        LambdaQueryWrapper<GroomingService> wrapper = new LambdaQueryWrapper<GroomingService>()
                .eq(GroomingService::getServiceCode, serviceCode.trim());
        if (excludeId != null) {
            wrapper.ne(GroomingService::getId, excludeId);
        }
        Long count = serviceMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "服务编码已存在");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return ENABLED;
        }
        String value = status.trim().toUpperCase();
        if (!ENABLED.equals(value) && !DISABLED.equals(value)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "状态仅支持 ENABLED/DISABLED");
        }
        return value;
    }

    private record Eligibility(boolean eligible, String reason) {
    }

}
