package com.team1ilchwiwoljang.domain.order.controller;

import com.team1ilchwiwoljang.common.security.WithMockAuthMember;
import com.team1ilchwiwoljang.common.config.SecurityConfig;
import com.team1ilchwiwoljang.common.security.JwtAuthenticationFilter;
import com.team1ilchwiwoljang.common.security.JwtTokenProvider;
import com.team1ilchwiwoljang.common.security.SecurityErrorResponseHandler;
import com.team1ilchwiwoljang.domain.order.dto.request.DirectOrderRequest;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderItemResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderResponse;
import com.team1ilchwiwoljang.domain.order.service.OrderService;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        SecurityErrorResponseHandler.class
})
class OrderControllerTest {

    private static final Long MEMBER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private MemberService memberService;

    @Test
    @WithMockAuthMember(memberId = 1L)
    @DisplayName("인증된 사용자가 올바른 주문 요청을 보내면 201 Created와 함께 주문 완료 응답을 반환한다")
    void given_validRequest_whenCreateDirectOrder_thenStatus201() throws Exception {
        // given
        DirectOrderRequest request = new DirectOrderRequest(1L, 2);
        OrderItemResponse itemResponse = new OrderItemResponse("상품명", 10000L, 2L, 20000L);
        OrderResponse response = new OrderResponse(1L, "order-123", "PENDING", 20000L, List.of(itemResponse));

        given(orderService.createDirectOrder(eq(MEMBER_ID), any(DirectOrderRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/orders/direct")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(1L))
                .andExpect(jsonPath("$.data.orderNumber").value("order-123"))
                .andExpect(jsonPath("$.data.orderStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.totalAmount").value(20000L))
                .andExpect(jsonPath("$.data.orderItems[0].productName").value("상품명"));
    }

    @Test
    @DisplayName("인증되지 않은 사용자가 요청을 보내면 401 Unauthorized를 반환한다")
    void given_noAuth_whenCreateDirectOrder_thenStatus401() throws Exception {
        // given
        DirectOrderRequest request = new DirectOrderRequest(1L, 2);

        // when & then
        mockMvc.perform(post("/api/orders/direct")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockAuthMember(memberId = 1L)
    @DisplayName("주문 요청 시 상품 ID가 누락되면 400 Bad Request를 반환한다")
    void given_nullProductId_whenCreateDirectOrder_thenStatus400() throws Exception {
        // given
        DirectOrderRequest request = new DirectOrderRequest(null, 2);

        // when & then
        mockMvc.perform(post("/api/orders/direct")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("productId"));
    }

    @Test
    @WithMockAuthMember(memberId = 1L)
    @DisplayName("주문 요청 시 주문 수량이 1 미만이면 400 Bad Request를 반환한다")
    void given_invalidQuantity_whenCreateDirectOrder_thenStatus400() throws Exception {
        // given
        DirectOrderRequest request = new DirectOrderRequest(1L, 0);

        // when & then
        mockMvc.perform(post("/api/orders/direct")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("quantity"));
    }
}
