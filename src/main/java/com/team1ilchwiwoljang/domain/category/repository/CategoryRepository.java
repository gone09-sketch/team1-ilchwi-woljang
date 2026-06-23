package com.team1ilchwiwoljang.domain.category.repository;

import com.team1ilchwiwoljang.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("""
            select distinct c
            from Category c
            left join fetch c.children
            where c.parent is null
            order by c.id asc
            """)
    List<Category> findAllRootWithChildren();
}
