package com.team1ilchwiwoljang.domain.category.controller;

import com.team1ilchwiwoljang.domain.product.dto.ProductResponse;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import com.team1ilchwiwoljang.domain.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private CategoryController categoryController;

    @Mock
    private ProductService productService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController).build();
    }

    @Test
    @DisplayName("카테고리별 상품 목록을 조회한다.")
    void getProductsByCategory() throws Exception {
        // given
        Long categoryId = 1L;
        List<ProductResponse> responses = List.of(
                new ProductResponse(1L, "티셔츠", 10000, 100, ProductStatus.ON_SALE, "편안한 티셔츠"),
                new ProductResponse(2L, "맨투맨", 20000, 50, ProductStatus.ON_SALE, "따뜻한 맨투맨")
        );

        given(productService.getProductsByCategory(eq(categoryId), anyString())).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/categories/{categoryId}/products", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("티셔츠"))
                .andExpect(jsonPath("$.data[1].name").value("맨투맨"));
    }
}
