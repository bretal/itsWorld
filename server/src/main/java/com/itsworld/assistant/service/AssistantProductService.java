package com.itsworld.assistant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.itsworld.assistant.dto.AssistantDtos.ProductCard;
import com.itsworld.commerce.catalog.service.ShopCatalogService;
import com.itsworld.commerce.catalog.dto.*;
import com.itsworld.commerce.catalog.mapper.ShopCategoryMapper;
import com.itsworld.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import static com.itsworld.assistant.service.AssistantErrors.error;

@Service
@RequiredArgsConstructor
public class AssistantProductService {
    private final ShopCatalogService catalog;
    private final ShopCategoryMapper categories;
    private final ObjectMapper json;

    public record SearchResult(List<ProductCard> products, boolean categoryFallback) {}

    public String categoryOptions() throws JsonProcessingException {
        return json.writeValueAsString(catalog.listEnabledCategories().stream()
                .map(category -> Map.of("id", category.getId().toString(), "name", category.getName(),
                        "parentId", Objects.toString(category.getParentId(), "")))
                .toList());
    }

    public SearchResult executeWithFallback(String name, JsonNode args, Long userId) {
        var matches = execute(name, args, userId);
        if (!matches.isEmpty() || !"search_products".equals(name)
                || !args.has("categoryId") || !args.path("allowCategoryFallback").asBoolean()) {
            return new SearchResult(matches, false);
        }
        var fallback = ((com.fasterxml.jackson.databind.node.ObjectNode) args).deepCopy().put("keyword", "");
        var alternatives = execute(name, fallback, userId);
        return new SearchResult(alternatives, !alternatives.isEmpty());
    }

    public List<ProductCard> execute(String name, JsonNode args, Long userId) {
        if (!args.isObject()) throw error("ASSISTANT_INVALID_TOOL_ARGS");
        if ("get_product".equals(name)) {
            allowed(args,Set.of("productId"));
            var card=get(positiveId(args.get("productId")),userId,null);
            return card==null ? List.of() : List.of(card);
        }
        if (!"search_products".equals(name)) throw error("ASSISTANT_INVALID_TOOL_ARGS");
        allowed(args,Set.of("keyword","categoryId","maxPriceYuan","allowCategoryFallback"));
        if (args.has("allowCategoryFallback") && !args.get("allowCategoryFallback").isBoolean()) {
            throw error("ASSISTANT_INVALID_TOOL_ARGS");
        }
        var keyword=args.get("keyword");
        if (keyword==null || !keyword.isTextual() || keyword.asText().length()>60) throw error("ASSISTANT_INVALID_TOOL_ARGS");
        Long category=args.has("categoryId") ? positiveId(args.get("categoryId")) : null;
        Long max=null;
        if (args.has("maxPriceYuan")) {
            if (!args.get("maxPriceYuan").isNumber()) throw error("ASSISTANT_INVALID_TOOL_ARGS");
            BigDecimal amount=args.get("maxPriceYuan").decimalValue();
            if (amount.signum()<0 || amount.compareTo(new BigDecimal("100000"))>0 || amount.stripTrailingZeros().scale()>2)
                throw error("ASSISTANT_INVALID_TOOL_ARGS");
            max=amount.movePointRight(2).longValueExact();
        }
        String searchTerm = keyword.asText().strip();
        Map<Long, ProductCard> result = new LinkedHashMap<>();
        boolean catSnacks = Set.of("猫零食", "猫咪零食", "猫用零食").contains(searchTerm);
        boolean dogSnacks = Set.of("狗零食", "狗狗零食", "犬用零食").contains(searchTerm);
        if (catSnacks || dogSnacks) {
            collectProducts(result, category, searchTerm, userId, max);
            if (!result.isEmpty()) return List.copyOf(result.values());
        }
        if (category == null && (catSnacks || dogSnacks || Set.of("宠物零食", "零食").contains(searchTerm))) {
            for (var snackCategory : catalog.listEnabledCategories()) {
                if (!Set.of("零食", "宠物零食").contains(snackCategory.getName())) continue;
                Map<Long, ProductCard> categoryMatches = new LinkedHashMap<>();
                collectProducts(categoryMatches, snackCategory.getId(), "", userId, max);
                for (var entry : categoryMatches.entrySet()) {
                    var card = entry.getValue();
                    String text = card.name() + " " + card.subtitle();
                    if (catSnacks && (text.contains("犬") || text.contains("狗"))) continue;
                    if (dogSnacks && text.contains("猫")) continue;
                    result.putIfAbsent(entry.getKey(), card);
                    if (result.size() == 3) return List.copyOf(result.values());
                }
            }
            if (!result.isEmpty()) return List.copyOf(result.values());
        }
        List<String> searchTerms = switch (searchTerm) {
            case "宠物零食", "零食" -> List.of(searchTerm, "零食", "冻干", "肉干", "磨牙");
            default -> List.of(searchTerm);
        };
        for (String term : new LinkedHashSet<>(searchTerms)) {
            collectProducts(result, category, term, userId, max);
            if (result.size() == 3) break;
        }
        return List.copyOf(result.values());
    }

    private void collectProducts(Map<Long, ProductCard> result, Long category, String term, Long userId, Long max) {
        int pageSize = 20;
        for (int page = 1; ; page++) {
            var candidates = catalog.pageProducts(category, term, page, pageSize, userId, true).getRecords();
            for (var candidate : candidates) {
                if (result.containsKey(candidate.getId())) continue;
                var card = get(candidate.getId(), userId, max);
                if (card != null) result.put(candidate.getId(), card);
                if (result.size() == 3) return;
            }
            if (candidates.size() < pageSize) return;
        }
    }

    private void allowed(JsonNode args,Set<String> allowed) {
        args.fieldNames().forEachRemaining(k->{if (!allowed.contains(k)) throw error("ASSISTANT_INVALID_TOOL_ARGS");});
    }
    private long positiveId(JsonNode value) {
        if (value==null || !(value.isIntegralNumber() || value.isTextual()) || !value.asText().matches("[1-9][0-9]{0,18}"))
            throw error("ASSISTANT_INVALID_TOOL_ARGS");
        try { return Long.parseLong(value.asText()); } catch (NumberFormatException e) { throw error("ASSISTANT_INVALID_TOOL_ARGS"); }
    }
    public ProductCard get(Long id,Long userId,Long max) {
        ShopProductDetailView detail;
        try { detail=catalog.getProductDetail(id,userId,true); } catch (BusinessException e) { return null; }
        if (!"ON_SALE".equals(detail.getSaleStatus())) return null;
        // Check all category ancestors, including cycles and missing/disabled parents.
        Long category=detail.getCategoryId(); Set<Long> seen=new HashSet<>();
        while (category!=null && category!=0) {
            if (!seen.add(category)) return null;
            var row=categories.selectById(category);
            if (row==null || !"ENABLED".equals(row.getStatus())) return null;
            category=row.getParentId();
        }
        var sku=detail.getSkus().stream().filter(s->"ENABLED".equals(s.getStatus()) && s.getAvailableQty()!=null
                && s.getAvailableQty()>0 && s.getSalePrice()!=null && s.getSalePrice()>=0
                && (max==null || s.getSalePrice()<=max)).min(Comparator.comparing(ShopSkuView::getSalePrice)).orElse(null);
        if (sku==null) return null;
        return new ProductCard(id.toString(),clean(detail.getName(),100),clean(detail.getSubtitle(),160),detail.getCoverUrl(),
                sku.getId().toString(),spec(sku.getSpecJson()),sku.getSalePrice(),sku.getMemberPrice(),sku.getAvailableQty(),
                "/pages/commerce/shop/detail?id="+id);
    }
    private String spec(String text) {
        try { JsonNode node=json.readTree(text); if (node.isObject()) {
            List<String> values=new ArrayList<>(); node.elements().forEachRemaining(v->values.add(clean(v.asText(),60)));
            return String.join(" / ",values);
        }} catch (Exception ignored) { }
        return clean(text,180);
    }
    private String clean(String value,int length) {
        if (value==null) return "";
        String text=value.replaceAll("<[^>]*>",""); return text.substring(0,Math.min(text.length(),length));
    }
}
