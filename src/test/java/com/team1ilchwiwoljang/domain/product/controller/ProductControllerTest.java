package com.team1ilchwiwoljang.domain.product.controller;

import com.team1ilchwiwoljang.common.config.SecurityConfig;
import com.team1ilchwiwoljang.common.exception.handler.GlobalExceptionHandler;
import com.team1ilchwiwoljang.common.security.JwtAuthenticationFilter;
import com.team1ilchwiwoljang.common.security.JwtTokenProvider;
import com.team1ilchwiwoljang.common.security.SecurityErrorResponseHandler;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import com.team1ilchwiwoljang.domain.product.dto.response.ProductSearchItemResponse;
import com.team1ilchwiwoljang.domain.product.dto.response.ProductSearchResponse;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import com.team1ilchwiwoljang.domain.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, SecurityErrorResponseHandler.class, GlobalExceptionHandler.class})
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
    void searchProductsReturnsSuccessResponse() throws Exception {
        ProductSearchResponse response = new ProductSearchResponse(
                List.of(new ProductSearchItemResponse(
                        1L,
                        "베이직 셔츠",
                        29_000,
                        10,
                        ProductStatus.ON_SALE,
                        true
                )),
                0,
                20,
                1,
                1,
                false
        );

        given(productService.searchProducts(eq("셔츠"), any(Pageable.class))).willReturn(response);

        mockMvc.perform(get("/api/products/search")
                        .param("keyword", "셔츠"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.products[0].productName").value("베이직 셔츠"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void searchProductsReturnsBadRequestWhenKeywordIsMissing() throws Exception {
        mockMvc.perform(get("/api/products/search"))
                .andExpect(status().isBadRequest());
    }
}
