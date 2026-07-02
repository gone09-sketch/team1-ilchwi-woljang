package com.team1ilchwiwoljang.domain.search.controller;

import com.team1ilchwiwoljang.common.config.SecurityConfig;
import com.team1ilchwiwoljang.common.exception.handler.GlobalExceptionHandler;
import com.team1ilchwiwoljang.common.security.JwtAuthenticationFilter;
import com.team1ilchwiwoljang.common.security.JwtTokenProvider;
import com.team1ilchwiwoljang.common.security.SecurityErrorResponseHandler;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import com.team1ilchwiwoljang.domain.search.dto.response.SearchKeywordPopularResponse;
import com.team1ilchwiwoljang.domain.search.service.SearchKeywordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchKeywordController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        SecurityErrorResponseHandler.class,
        GlobalExceptionHandler.class
})
class SearchKeywordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchKeywordService searchKeywordService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private MemberService memberService;

    @Test
    @DisplayName("인기 검색어 조회 시 200 OK와 검색어 목록을 반환한다.")
    void given_validLimit_whenGetPopularKeywords_thenStatus200() throws Exception {
        List<SearchKeywordPopularResponse> response = List.of(
                new SearchKeywordPopularResponse("셔츠", 100L),
                new SearchKeywordPopularResponse("청바지", 80L),
                new SearchKeywordPopularResponse("원피스", 60L)
        );

        given(searchKeywordService.getPopularKeywords(anyInt())).willReturn(response);

        mockMvc.perform(get("/api/search/popular").param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].keyword").value("셔츠"))
                .andExpect(jsonPath("$.data[0].searchCount").value(100))
                .andExpect(jsonPath("$.data[1].keyword").value("청바지"))
                .andExpect(jsonPath("$.data[2].keyword").value("원피스"));
    }

    @Test
    @DisplayName("인기 검색어 조회 시 검색어가 없으면 빈 리스트를 반환한다.")
    void given_noKeywords_whenGetPopularKeywords_thenReturnEmptyList() throws Exception {
        given(searchKeywordService.getPopularKeywords(anyInt())).willReturn(List.of());

        mockMvc.perform(get("/api/search/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("limit이 100을 초과하면 400 Bad Request를 반환한다.")
    void given_limitExceedingMax_whenGetPopularKeywords_thenStatus400() throws Exception {
        mockMvc.perform(get("/api/search/popular").param("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("limit"));
    }

    @Test
    @DisplayName("limit이 1 미만이면 400 Bad Request를 반환한다.")
    void given_limitUnderMin_whenGetPopularKeywords_thenStatus400() throws Exception {
        mockMvc.perform(get("/api/search/popular").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("limit"));
    }
}
