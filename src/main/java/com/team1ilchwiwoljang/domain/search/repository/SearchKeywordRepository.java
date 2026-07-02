package com.team1ilchwiwoljang.domain.search.repository;

import com.team1ilchwiwoljang.domain.search.entity.SearchKeyword;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SearchKeywordRepository extends JpaRepository<SearchKeyword, Long> {

    Optional<SearchKeyword> findByKeyword(String keyword);

    List<SearchKeyword> findAllByOrderBySearchCountDesc(Pageable pageable);
}
