package com.team1ilchwiwoljang.domain.inquiry.controller;

import com.team1ilchwiwoljang.common.config.SecurityConfig;
import com.team1ilchwiwoljang.common.security.JwtAuthenticationFilter;
import com.team1ilchwiwoljang.common.security.JwtTokenProvider;
import com.team1ilchwiwoljang.common.security.SecurityErrorResponseHandler;
import org.springframework.http.HttpHeaders;
import tools.jackson.databind.ObjectMapper;
import com.team1ilchwiwoljang.domain.inquiry.dto.request.InquiryCreateRequest;
import com.team1ilchwiwoljang.domain.inquiry.dto.response.InquiryCreateResponse;
import com.team1ilchwiwoljang.domain.inquiry.entity.InquiryStatus;
import com.team1ilchwiwoljang.domain.inquiry.service.InquiryService;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InquiryController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        SecurityErrorResponseHandler.class
})
class InquiryControllerTest {

    private static final Long MEMBER_ID = 1L;
    private static final String ACCESS_TOKEN = "valid-access-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InquiryService inquiryService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private MemberService memberService;

    @Test
    @DisplayName("인증된 사용자가 올바른 요청을 보내면 201 Created와 함께 문의 응답을 반환한다")
    void given_validRequest_whenCreateInquiry_thenStatus201() throws Exception {
        // given
        InquiryCreateRequest request = new InquiryCreateRequest("문의 제목", "문의 내용");

        InquiryCreateResponse response = new InquiryCreateResponse(
                1L,
                MEMBER_ID,
                "문의 제목",
                "문의 내용",
                InquiryStatus.WAITING,
                LocalDateTime.now()
        );

        given(jwtTokenProvider.getMemberId(ACCESS_TOKEN)).willReturn(MEMBER_ID);
        given(memberService.findById(MEMBER_ID)).willReturn(Optional.of(mock(Member.class)));

        given(inquiryService.createInquiry(eq(MEMBER_ID), any(InquiryCreateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/members/inquiry")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
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
    @DisplayName("문의 제목이 빈 값이면 400 Bad Request를 반환한다")
    void given_blankTitle_whenCreateInquiry_thenStatus400() throws Exception {
        // given
        InquiryCreateRequest request = new InquiryCreateRequest("", "문의 내용");

        given(jwtTokenProvider.getMemberId(ACCESS_TOKEN)).willReturn(MEMBER_ID);
        given(memberService.findById(MEMBER_ID)).willReturn(Optional.of(mock(Member.class)));

        // when & then
        mockMvc.perform(post("/api/members/inquiry")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("title"));
    }

    @Test
    @DisplayName("문의 내용이 빈 값이면 400 Bad Request를 반환한다")
    void given_blankContent_whenCreateInquiry_thenStatus400() throws Exception {
        // given
        InquiryCreateRequest request = new InquiryCreateRequest("문의 제목", " ");

        given(jwtTokenProvider.getMemberId(ACCESS_TOKEN)).willReturn(MEMBER_ID);
        given(memberService.findById(MEMBER_ID)).willReturn(Optional.of(mock(Member.class)));

        // when & then
        mockMvc.perform(post("/api/members/inquiry")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
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
