package com.team1ilchwiwoljang.domain.search.repository;

import com.team1ilchwiwoljang.domain.search.entity.SearchKeyword;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SearchKeywordRepository extends JpaRepository<SearchKeyword, Long> {

    Optional<SearchKeyword> findByKeyword(String keyword);

    List<SearchKeyword> findAllByOrderBySearchCountDesc(Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("update SearchKeyword sk set sk.searchCount = sk.searchCount + 1 where sk.keyword = :keyword")
    int incrementSearchCount(@Param("keyword") String keyword);
}
