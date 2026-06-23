package com.team1ilchwiwoljang.domain.category.entity;

import com.team1ilchwiwoljang.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private List<Category> children = new ArrayList<>();

    public static Category createRoot(String name) {
        Category category = new Category();
        category.name = name;
        return category;
    }

    public static Category createChild(String name, Category parent) {
        Category category = new Category();
        category.name = name;
        category.parent = parent;
        return category;
    }
}
