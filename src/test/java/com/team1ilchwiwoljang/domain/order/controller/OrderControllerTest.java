package com.team1ilchwiwoljang.domain.order.controller;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.common.config.SecurityConfig;
import com.team1ilchwiwoljang.common.security.JwtAuthenticationFilter;
import com.team1ilchwiwoljang.common.security.JwtTokenProvider;
import com.team1ilchwiwoljang.common.security.SecurityErrorResponseHandler;
import com.team1ilchwiwoljang.common.security.WithMockAuthMember;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import com.team1ilchwiwoljang.domain.order.dto.request.DirectOrderPreviewRequest;
import com.team1ilchwiwoljang.domain.order.dto.request.DirectOrderRequest;
import com.team1ilchwiwoljang.domain.order.dto.response.DirectOrderPreviewResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderItemResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderPreviewResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderResponse;
import com.team1ilchwiwoljang.domain.order.service.OrderService;
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
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @DisplayName("인증된 사용자가 올바른 바로 구매 미리보기 요청을 보내면 200 OK와 함께 미리보기 응답을 반환한다")
    void given_validRequest_whenPreviewDirectOrder_thenStatus200() throws Exception {
        // given
        DirectOrderPreviewRequest request = new DirectOrderPreviewRequest(1L, 2);
        DirectOrderPreviewResponse.DirectOrderPreviewItemResponse itemResponse =
                DirectOrderPreviewResponse.DirectOrderPreviewItemResponse.of(1L, "상품명", 10000L, 2, 20000L);
        DirectOrderPreviewResponse response = DirectOrderPreviewResponse.of(List.of(itemResponse), 20000L);

        given(orderService.previewDirectOrder(any(DirectOrderPreviewRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/orders/direct/preview")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalOrderAmount").value(20000L))
                .andExpect(jsonPath("$.data.orderItems[0].productId").value(1L))
                .andExpect(jsonPath("$.data.orderItems[0].productName").value("상품명"))
                .andExpect(jsonPath("$.data.orderItems[0].productPrice").value(10000L))
                .andExpect(jsonPath("$.data.orderItems[0].quantity").value(2))
                .andExpect(jsonPath("$.data.orderItems[0].productTotalAmount").value(20000L));
    }

    @Test
    @DisplayName("인증되지 않은 사용자가 바로 구매 미리보기를 요청하면 401 Unauthorized를 반환한다")
    void given_noAuth_whenPreviewDirectOrder_thenStatus401() throws Exception {
        // given
        DirectOrderPreviewRequest request = new DirectOrderPreviewRequest(1L, 2);

        // when & then
        mockMvc.perform(post("/api/orders/direct/preview")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockAuthMember(memberId = 1L)
    @DisplayName("바로 구매 미리보기 요청 시 주문 수량이 1 미만이면 400 Bad Request를 반환한다")
    void given_invalidQuantity_whenPreviewDirectOrder_thenStatus400() throws Exception {
        // given
        DirectOrderPreviewRequest request = new DirectOrderPreviewRequest(1L, 0);

        // when & then
        mockMvc.perform(post("/api/orders/direct/preview")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("quantity"));
    }

    @Test
    @WithMockAuthMember(memberId = 1L)
    @DisplayName("인증된 사용자가 장바구니 주문 미리보기를 요청하면 200 OK와 함께 미리보기 응답을 반환한다")
    void given_cartIds_whenPreviewOrder_thenStatus200() throws Exception {
        // given
        List<Long> cartIds = List.of(10L, 20L);
        List<OrderPreviewResponse.OrderPreviewItemResponse> orderItems = List.of(
                OrderPreviewResponse.OrderPreviewItemResponse.of(10L, 100L, "상품A", 10000L, 2, 20000L),
                OrderPreviewResponse.OrderPreviewItemResponse.of(20L, 200L, "상품B", 5000L, 1, 5000L)
        );
        OrderPreviewResponse response = OrderPreviewResponse.of(orderItems, 25000L);

        // MockMvc가 전달한 cartIds 요청 파라미터가 List<Long>으로 변환되어 Service에 전달되는지 함께 확인합니다.
        given(orderService.previewOrder(eq(MEMBER_ID), eq(cartIds))).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/orders/preview")
                        .param("cartIds", "10", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data.totalOrderAmount").value(25000L))
                .andExpect(jsonPath("$.data.orderItems").isArray())
                .andExpect(jsonPath("$.data.orderItems[0].cartId").value(10L))
                .andExpect(jsonPath("$.data.orderItems[0].productId").value(100L))
                .andExpect(jsonPath("$.data.orderItems[0].productName").value("상품A"))
                .andExpect(jsonPath("$.data.orderItems[0].productPrice").value(10000L))
                .andExpect(jsonPath("$.data.orderItems[0].quantity").value(2))
                .andExpect(jsonPath("$.data.orderItems[0].productTotalAmount").value(20000L))
                .andExpect(jsonPath("$.data.orderItems[1].cartId").value(20L))
                .andExpect(jsonPath("$.data.orderItems[1].productId").value(200L))
                .andExpect(jsonPath("$.data.orderItems[1].productName").value("상품B"))
                .andExpect(jsonPath("$.data.orderItems[1].productPrice").value(5000L))
                .andExpect(jsonPath("$.data.orderItems[1].quantity").value(1))
                .andExpect(jsonPath("$.data.orderItems[1].productTotalAmount").value(5000L));
    }

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
                .andExpect(jsonPath("$.data.orderItems[0].productName").value("상품명"))
                .andExpect(jsonPath("$.data.orderItems[0].productPrice").value(10000L))
                .andExpect(jsonPath("$.data.orderItems[0].quantity").value(2L))
                .andExpect(jsonPath("$.data.orderItems[0].totalPrice").value(20000L));
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
    @DisplayName("주문 요청 시 주문 수량이 누락되면 400 Bad Request를 반환한다")
    void given_nullQuantity_whenCreateDirectOrder_thenStatus400() throws Exception {
        // given
        DirectOrderRequest request = new DirectOrderRequest(1L, null);

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

    @Test
    @WithMockAuthMember(memberId = 1L)
    @DisplayName("인증된 주문 소유자가 취소 요청을 보내면 200 OK를 반환한다")
    void given_authenticatedOwner_whenCancelOrder_thenStatus200() throws Exception {
        // given
        Long orderId = 1L;

        // when & then
        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockAuthMember(memberId = 1L)
    @DisplayName("존재하지 않거나 소유자가 다른 주문 취소 요청은 404 Not Found를 반환한다")
    void given_notOwnedOrder_whenCancelOrder_thenStatus404() throws Exception {
        // given
        Long orderId = 999L;

        doThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND))
                .when(orderService)
                .cancelOrder(eq(MEMBER_ID), eq(orderId));

        // when & then
        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    @DisplayName("인증되지 않은 사용자가 주문 취소 요청을 보내면 401 Unauthorized를 반환한다")
    void given_noAuth_whenCancelOrder_thenStatus401() throws Exception {
        // when & then
        mockMvc.perform(post("/api/orders/{orderId}/cancel", 1L)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockAuthMember(memberId = 1L)
    @DisplayName("이미 취소된 주문을 다시 취소하면 400 Bad Request를 반환한다")
    void given_cancelledOrder_whenCancelAgain_thenStatus400() throws Exception {
        // given
        Long orderId = 1L;

        doThrow(new BusinessException(ErrorCode.INVALID_ORDER_STATUS))
                .when(orderService)
                .cancelOrder(eq(MEMBER_ID), eq(orderId));

        // when & then
        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATUS"));
    }
}
