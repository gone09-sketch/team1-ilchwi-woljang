package com.team1ilchwiwoljang.domain.search.controller;

import com.team1ilchwiwoljang.common.response.ApiResponse;
import com.team1ilchwiwoljang.domain.search.dto.response.PopularKeywordResponse;
import com.team1ilchwiwoljang.domain.search.service.SearchKeywordService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class SearchKeywordController {

    private final SearchKeywordService searchKeywordService;

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<PopularKeywordResponse>>> getPopularKeywords(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        List<PopularKeywordResponse> response = searchKeywordService.getPopularKeywords(limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
