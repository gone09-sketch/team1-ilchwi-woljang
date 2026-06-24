package com.team1ilchwiwoljang.domain.inquiry.repository;

import com.team1ilchwiwoljang.domain.inquiry.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
}
