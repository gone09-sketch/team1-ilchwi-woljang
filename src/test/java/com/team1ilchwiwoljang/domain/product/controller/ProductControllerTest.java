package com.team1ilchwiwoljang.domain.product.controller;

import com.team1ilchwiwoljang.common.config.SecurityConfig;
import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.common.exception.handler.GlobalExceptionHandler;
import com.team1ilchwiwoljang.common.security.JwtAuthenticationFilter;
import com.team1ilchwiwoljang.common.security.JwtTokenProvider;
import com.team1ilchwiwoljang.common.security.SecurityErrorResponseHandler;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import com.team1ilchwiwoljang.domain.product.dto.ProductResponse;
import com.team1ilchwiwoljang.domain.product.dto.response.ProductDetailResponse;
import com.team1ilchwiwoljang.domain.product.dto.response.PopularProductResponse;
import com.team1ilchwiwoljang.domain.product.dto.response.ProductSearchItemResponse;
import com.team1ilchwiwoljang.common.response.PageResponse;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import com.team1ilchwiwoljang.domain.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        SecurityErrorResponseHandler.class,
        GlobalExceptionHandler.class
})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private MemberService memberService;

    @Test
    @DisplayName("상품 검색 요청 시 200 OK와 검색 결과를 반환한다.")
    void searchProductsReturnsSuccessResponse() throws Exception {
        PageResponse<ProductSearchItemResponse> response = new PageResponse<>(
                List.of(new ProductSearchItemResponse(
                        1L,
                        "베이직 셔츠",
                        29_000,
                        10,
                        ProductStatus.ON_SALE,
                        true,
                        "/images/products/bag-01.png"
                )),
                0,
                20,
                1,
                1,
                true
        );

        given(productService.searchProducts(eq("셔츠"), any(Pageable.class)))
                .willReturn(response);

        mockMvc.perform(get("/api/products/search")
                        .param("keyword", "셔츠"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].productId").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("베이직 셔츠"))
                .andExpect(jsonPath("$.data.content[0].price").value(29000))
                .andExpect(jsonPath("$.data.content[0].stock").value(10))
                .andExpect(jsonPath("$.data.content[0].status").value("ON_SALE"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    @DisplayName("상품 목록을 조회하면 200 OK와 페이지네이션 응답을 반환한다.")
    void given_validParams_whenGetProducts_thenStatus200() throws Exception {
        List<ProductResponse> products = List.of(
                new ProductResponse(1L, "티셔츠", 10000, 100, ProductStatus.ON_SALE, "편안한 티셔츠", "/images/products/bag-01.png"),
                new ProductResponse(2L, "맨투맨", 20000, 50, ProductStatus.ON_SALE, "따뜻한 맨투맨", "/images/products/bag-02.png")
        );

        given(productService.getProducts(anyString(), anyInt(), anyInt()))
                .willReturn(new PageImpl<>(products));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].productId").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("티셔츠"))
                .andExpect(jsonPath("$.data.content[1].productId").value(2))
                .andExpect(jsonPath("$.data.content[1].name").value("맨투맨"));
    }

    @Test
    @DisplayName("검색 키워드가 없으면 400 Bad Request를 반환한다.")
    void searchProductsReturnsBadRequestWhenKeywordIsMissing() throws Exception {
        mockMvc.perform(get("/api/products/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하는 상품 ID로 요청하면 200 OK와 상품 상세 정보를 반환한다.")
    void getProductDetail_existingProduct_returnsProductDetail() throws Exception {
        Long productId = 1L;

        ProductDetailResponse response = new ProductDetailResponse(
                productId,
                "티셔츠",
                "편안한 티셔츠",
                10000,
                100,
                ProductStatus.ON_SALE,
                1L,
                "상의",
                "/images/products/bag-01.png"
        );

        given(productService.getProductDetail(productId))
                .willReturn(response);

        mockMvc.perform(get("/api/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(1))
                .andExpect(jsonPath("$.data.name").value("티셔츠"))
                .andExpect(jsonPath("$.data.description").value("편안한 티셔츠"))
                .andExpect(jsonPath("$.data.price").value(10000))
                .andExpect(jsonPath("$.data.stock").value(100))
                .andExpect(jsonPath("$.data.status").value("ON_SALE"))
                .andExpect(jsonPath("$.data.categoryId").value(1))
                .andExpect(jsonPath("$.data.categoryName").value("상의"));
    }

    @Test
    @DisplayName("존재하지 않는 상품 ID로 요청하면 404 Not Found를 반환한다.")
    void getProductDetail_missingProduct_returnsNotFound() throws Exception {
        Long productId = 999L;

        given(productService.getProductDetail(productId))
                .willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        mockMvc.perform(get("/api/products/{productId}", productId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("인기 상품 목록을 조회하면 200 OK와 인기 상품 목록을 반환한다.")
    void given_validLimit_whenGetPopularProducts_thenStatus200() throws Exception {
        int limit = 2;
        List<PopularProductResponse> response = List.of(
                new PopularProductResponse(1L, "티셔츠", 10000, 100, ProductStatus.ON_SALE, 50, "/images/products/bag-01.png"),
                new PopularProductResponse(2L, "맨투맨", 20000, 50, ProductStatus.ON_SALE, 30, "/images/products/bag-02.png")
        );

        given(productService.getPopularProducts(limit)).willReturn(response);

        mockMvc.perform(get("/api/products/popular").param("limit", String.valueOf(limit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].productId").value(1))
                .andExpect(jsonPath("$.data[0].name").value("티셔츠"))
                .andExpect(jsonPath("$.data[0].salesCount").value(50))
                .andExpect(jsonPath("$.data[1].productId").value(2))
                .andExpect(jsonPath("$.data[1].name").value("맨투맨"))
                .andExpect(jsonPath("$.data[1].salesCount").value(30));
    }

    @Test
    @DisplayName("인기 상품 조회 시 limit이 100을 초과하면 400 Bad Request를 반환한다.")
    void given_limitExceedingMax_whenGetPopularProducts_thenStatus400() throws Exception {
        mockMvc.perform(get("/api/products/popular").param("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("limit"));
    }

    @Test
    @DisplayName("인기 상품 조회 시 limit이 1 미만이면 400 Bad Request를 반환한다.")
    void given_limitUnderMin_whenGetPopularProducts_thenStatus400() throws Exception {
        mockMvc.perform(get("/api/products/popular").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("limit"));
    }

    @Test
    @DisplayName("가격 범위 조회 시 minPrice가 음수면 400 Bad Request를 반환한다.")
    void given_negativeMinPrice_whenGetProductsByPriceRange_thenStatus400() throws Exception {
        mockMvc.perform(get("/api/products/price-range")
                        .param("minPrice", "-1")
                        .param("maxPrice", "10000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("minPrice"));
    }

    @Test
    @DisplayName("가격 범위 조회 시 minPrice가 maxPrice보다 크면 400 Bad Request를 반환한다.")
    void given_minPriceGreaterThanMaxPrice_whenGetProductsByPriceRange_thenStatus400() throws Exception {
        given(productService.getProductsByPriceRange(20000, 10000, 0, 20))
                .willThrow(new BusinessException(ErrorCode.INVALID_PRICE_RANGE));

        mockMvc.perform(get("/api/products/price-range")
                        .param("minPrice", "20000")
                        .param("maxPrice", "10000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_PRICE_RANGE"));
    }
}