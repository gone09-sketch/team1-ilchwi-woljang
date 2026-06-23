package com.team1ilchwiwoljang.domain.inquiry.controller;

import tools.jackson.databind.ObjectMapper;
import com.team1ilchwiwoljang.domain.inquiry.dto.request.InquiryCreateRequest;
import com.team1ilchwiwoljang.domain.inquiry.dto.response.InquiryCreateResponse;
import com.team1ilchwiwoljang.domain.inquiry.entity.InquiryStatus;
import com.team1ilchwiwoljang.domain.inquiry.service.InquiryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InquiryController.class)
class InquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InquiryService inquiryService;

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("인증된 사용자가 올바른 요청을 보내면 201 Created와 함께 문의 응답을 반환한다")
    void given_validRequest_whenCreateInquiry_thenStatus201() throws Exception {
        // given
        InquiryCreateRequest request = new InquiryCreateRequest("문의 제목", "문의 내용");
        InquiryCreateResponse response = new InquiryCreateResponse(
                1L,
                1L,
                "문의 제목",
                "문의 내용",
                InquiryStatus.WAITING,
                LocalDateTime.now()
        );

        given(inquiryService.createInquiry(eq("test@example.com"), any(InquiryCreateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/members/inquiry")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.title").value("문의 제목"))
                .andExpect(jsonPath("$.data.content").value("문의 내용"))
                .andExpect(jsonPath("$.data.status").value("WAITING"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("문의 제목이 빈 값이면 400 Bad Request를 반환한다")
    void given_blankTitle_whenCreateInquiry_thenStatus400() throws Exception {
        // given
        InquiryCreateRequest request = new InquiryCreateRequest("", "문의 내용");

        // when & then
        mockMvc.perform(post("/api/members/inquiry")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("title"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("문의 내용이 빈 값이면 400 Bad Request를 반환한다")
    void given_blankContent_whenCreateInquiry_thenStatus400() throws Exception {
        // given
        InquiryCreateRequest request = new InquiryCreateRequest("문의 제목", " ");

        // when & then
        mockMvc.perform(post("/api/members/inquiry")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("content"));
    }

    @Test
    @DisplayName("인증되지 않은 사용자가 요청을 보내면 401 Unauthorized를 반환한다")
    void given_noAuth_whenCreateInquiry_thenStatus401() throws Exception {
        // given
        InquiryCreateRequest request = new InquiryCreateRequest("문의 제목", "문의 내용");

        // when & then
        mockMvc.perform(post("/api/members/inquiry")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
