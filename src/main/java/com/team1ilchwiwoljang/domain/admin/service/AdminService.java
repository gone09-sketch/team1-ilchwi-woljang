package com.team1ilchwiwoljang.domain.admin.service;

import com.team1ilchwiwoljang.domain.admin.entity.Admin;
import com.team1ilchwiwoljang.domain.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final AdminRepository adminRepository;

    public Optional<Admin> findById(Long id) {
        return adminRepository.findById(id);
    }
}
