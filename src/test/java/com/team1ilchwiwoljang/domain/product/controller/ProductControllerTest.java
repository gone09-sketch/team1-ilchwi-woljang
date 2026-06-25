package com.team1ilchwiwoljang.domain.product.controller;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.common.exception.handler.GlobalExceptionHandler;
import com.team1ilchwiwoljang.domain.product.dto.ProductResponse;
import com.team1ilchwiwoljang.domain.product.dto.response.ProductDetailResponse;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import com.team1ilchwiwoljang.domain.product.service.ProductService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private ProductController productController;

    @Mock
    private ProductService productService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("상품 목록을 조회하면 200 OK와 페이지네이션 응답을 반환한다.")
    void given_validParams_whenGetProducts_thenStatus200() throws Exception {
        // given
        List<ProductResponse> products = List.of(
                new ProductResponse(1L, "티셔츠", 10000, 100, ProductStatus.ON_SALE, "편안한 티셔츠"),
                new ProductResponse(2L, "맨투맨", 20000, 50, ProductStatus.ON_SALE, "따뜻한 맨투맨")
        );
        given(productService.getProducts(anyString(), anyInt(), anyInt()))
                .willReturn(new PageImpl<>(products));

        // when & then
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].productId").value(1L))
                .andExpect(jsonPath("$.data.content[0].name").value("티셔츠"))
                .andExpect(jsonPath("$.data.content[1].productId").value(2L))
                .andExpect(jsonPath("$.data.content[1].name").value("맨투맨"));
    }

    @Test
    @DisplayName("존재하는 상품 ID로 요청하면 200 OK와 상품 상세 정보를 반환한다.")
    void given_existingProductId_whenGetProductDetail_thenStatus200() throws Exception {
        // given
        Long productId = 1L;
        ProductDetailResponse response = new ProductDetailResponse(
                productId, "티셔츠", "편안한 티셔츠", 10000, 100, ProductStatus.ON_SALE, 1L, "상의"
        );
        given(productService.getProductDetail(productId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(productId))
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
    void given_nonExistentProductId_whenGetProductDetail_thenStatus404() throws Exception {
        // given
        Long productId = 999L;
        given(productService.getProductDetail(productId))
                .willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/products/{productId}", productId))
                .andExpect(status().isNotFound());
    }
}
