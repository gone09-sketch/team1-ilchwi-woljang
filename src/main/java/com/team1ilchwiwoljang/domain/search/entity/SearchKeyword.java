package com.team1ilchwiwoljang.domain.search.entity;

import com.team1ilchwiwoljang.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "search_keyword")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchKeyword extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String keyword;

    @Column(nullable = false)
    private long searchCount;

    /**
     * 신규 검색어를 생성합니다. 초기 카운트는 1입니다.
     */
    public static SearchKeyword create(String keyword) {
        SearchKeyword sk = new SearchKeyword();
        sk.keyword = keyword;
        sk.searchCount = 1L;
        return sk;
    }

    /**
     * 검색 횟수를 1 증가시킵니다.
     */
    public void incrementCount() {
        this.searchCount++;
    }
}
