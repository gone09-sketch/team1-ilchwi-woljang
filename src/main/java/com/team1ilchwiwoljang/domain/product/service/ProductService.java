package com.team1ilchwiwoljang.domain.product.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Product getProduct(Long productId){
        return productRepository.findById(productId)
            .orElseThrow(()-> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        }
    }

