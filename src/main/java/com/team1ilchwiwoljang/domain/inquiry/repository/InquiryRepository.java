package com.team1ilchwiwoljang.domain.inquiry.repository;

import com.team1ilchwiwoljang.domain.inquiry.entity.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    @EntityGraph(attributePaths = "member")
    Page<Inquiry> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
