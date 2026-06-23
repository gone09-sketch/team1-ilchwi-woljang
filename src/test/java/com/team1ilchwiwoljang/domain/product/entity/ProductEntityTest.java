package com.team1ilchwiwoljang.domain.product.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.team1ilchwiwoljang.common.entity.BaseEntity;
import org.junit.jupiter.api.Test;

class ProductEntityTest {

    @Test
    void productExtendsBaseEntity() {
        assertThat(BaseEntity.class.isAssignableFrom(Product.class)).isTrue();
    }
}
