package com.itsworld.assistant;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsworld.assistant.service.AssistantProductService;
import com.itsworld.commerce.catalog.service.ShopCatalogService;
import com.itsworld.commerce.catalog.mapper.ShopCategoryMapper;
import com.itsworld.commerce.catalog.entity.ShopCategory;
import com.itsworld.commerce.catalog.dto.*;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.PageResult;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AssistantProductServiceTest {
    ShopCatalogService catalog=mock(ShopCatalogService.class);
    ShopCategoryMapper categories=mock(ShopCategoryMapper.class);
    ObjectMapper json=new ObjectMapper();
    AssistantProductService service=new AssistantProductService(catalog,categories,json);
    ShopProductDetailView detail;
    @BeforeEach void setup() {
        var unavailable=ShopSkuView.builder().id(1L).salePrice(100L).status("ENABLED").availableQty(0).build();
        var available=ShopSkuView.builder().id(2L).salePrice(3900L).memberPrice(3700L).specJson("{\"口味\":\"鸡肉\"}").status("ENABLED").availableQty(5).build();
        detail=ShopProductDetailView.builder().id(10L).categoryId(3L).name("真实商品").saleStatus("ON_SALE").skus(List.of(unavailable,available)).build();
        when(catalog.getProductDetail(10L,7L,true)).thenReturn(detail);
        when(categories.selectById(3L)).thenReturn(ShopCategory.builder().id(3L).status("ENABLED").build());
        when(catalog.pageProducts(null,"冻干",1,20,7L,true)).thenReturn(PageResult.of(List.of(ShopProductView.builder().id(10L).build()),1,1,20));
    }
    @Test void selectsInStockPriceAndMemberPrice() {
        var card=service.get(10L,7L,null); assertEquals("2",card.skuId()); assertEquals(3900,card.priceCents());
        assertEquals(3700,card.memberPriceCents()); assertEquals("鸡肉",card.specText()); assertEquals(5,card.availableQty());
        assertNull(service.get(10L,7L,3000L));
    }
    @Test void categoryFallbackPreservesBudgetAndRequiresOptIn() throws Exception {
        when(catalog.pageProducts(3L,"指定冻干",1,20,7L,true))
                .thenReturn(PageResult.of(List.of(),0,1,20));
        when(catalog.pageProducts(3L,"",1,20,7L,true))
                .thenReturn(PageResult.of(List.of(ShopProductView.builder().id(10L).build()),1,1,20));
        var args = json.createObjectNode().put("keyword","指定冻干").put("categoryId","3").put("maxPriceYuan",40);
        assertTrue(service.executeWithFallback("search_products",args,7L).products().isEmpty());
        args.put("allowCategoryFallback",true);
        var result = service.executeWithFallback("search_products",args,7L);
        assertTrue(result.categoryFallback());
        assertEquals("10",result.products().get(0).id());
        args.put("maxPriceYuan",30);
        assertTrue(service.executeWithFallback("search_products",args,7L).products().isEmpty());
    }
    @Test void disabledCategoryAndSkuAndNoStockAreRejected() {
        when(categories.selectById(3L)).thenReturn(ShopCategory.builder().status("DISABLED").build()); assertNull(service.get(10L,7L,null));
        when(categories.selectById(3L)).thenReturn(ShopCategory.builder().status("ENABLED").build());
        detail.getSkus().get(1).setStatus("DISABLED"); assertNull(service.get(10L,7L,null));
        detail.getSkus().get(1).setStatus("ENABLED"); detail.getSkus().get(1).setAvailableQty(0); assertNull(service.get(10L,7L,null));
        detail.setSaleStatus("OFF_SALE"); assertNull(service.get(10L,7L,null));
    }
    @Test void strictArgumentsAndBudget() throws Exception {
        for (String args:List.of("{\"keyword\":true}","{\"keyword\":\"x\",\"maxPriceYuan\":1.001}","{\"keyword\":\"x\",\"maxPriceYuan\":100001}","{\"keyword\":\"x\",\"categoryId\":-1}","{\"keyword\":\"x\",\"extra\":1}")) {
            var node=json.readTree(args); assertThrows(BusinessException.class,()->service.execute("search_products",node,7L));
        }
        assertTrue(service.execute("search_products",json.readTree("{\"keyword\":\"冻干\",\"maxPriceYuan\":30}"),7L).isEmpty());
        assertEquals(1,service.execute("search_products",json.readTree("{\"keyword\":\"冻干\",\"maxPriceYuan\":40}"),7L).size());
        assertThrows(BusinessException.class,()->service.execute("unknown",json.createObjectNode(),7L));
        assertThrows(BusinessException.class,()->service.execute("get_product",json.readTree("{\"productId\":\"9223372036854775808\"}"),7L));
    }
    @Test void genericSnackSearchFindsFreezeDriedStockWithoutDroppingBudget() {
        for (String term : List.of("宠物零食", "零食", "肉干", "磨牙")) {
            when(catalog.pageProducts(null, term, 1, 20, 7L, true))
                    .thenReturn(PageResult.of(List.of(), 0, 1, 20));
        }
        var args = json.createObjectNode().put("keyword", "宠物零食").put("maxPriceYuan", 100);
        assertEquals("10", service.execute("search_products", args, 7L).get(0).id());
        args.put("maxPriceYuan", 30);
        assertTrue(service.execute("search_products", args, 7L).isEmpty());
    }
    @Test void catSnackQueryFallsBackToSnackCategoryWithinBudget() {
        when(catalog.pageProducts(null, "猫零食", 1, 20, 7L, true))
                .thenReturn(PageResult.of(List.of(), 0, 1, 20));
        when(catalog.listEnabledCategories()).thenReturn(List.of(
                com.itsworld.commerce.catalog.entity.ShopCategory.builder().id(3L).name("零食").build()));
        var categoryPage = catalog.pageProducts(null, "冻干", 1, 20, 7L, true);
        when(catalog.pageProducts(3L, "", 1, 20, 7L, true)).thenReturn(categoryPage);
        var args = json.createObjectNode().put("keyword", "猫零食").put("maxPriceYuan", 100);
        assertEquals("10", service.execute("search_products", args, 7L).get(0).id());
        args.put("maxPriceYuan", 30);
        assertTrue(service.execute("search_products", args, 7L).isEmpty());
    }
}
