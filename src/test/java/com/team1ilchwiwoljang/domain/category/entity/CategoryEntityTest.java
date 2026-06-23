package com.team1ilchwiwoljang.domain.category.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CategoryEntityTest {

    @Test
    @DisplayName("createRoot()는 parent가 없는 루트 카테고리를 생성한다")
    void givenName_whenCreateRoot_thenParentIsNull() {
        Category root = Category.createRoot("패션");

        assertThat(root.getName()).isEqualTo("패션");
        assertThat(root.getParent()).isNull();
        assertThat(root.getChildren()).isEmpty();
    }

    @Test
    @DisplayName("createChild()는 child.parent와 parent.children 양방향 관계를 모두 설정한다")
    void givenParent_whenCreateChild_thenBidirectionalRelationIsSet() {
        Category root = Category.createRoot("패션");

        Category child = Category.createChild("상의", root);

        assertThat(child.getParent()).isSameAs(root);
        assertThat(root.getChildren()).containsExactly(child);
    }

    @Test
    @DisplayName("여러 자식을 추가하면 parent.children에 모두 포함된다")
    void givenParent_whenCreateMultipleChildren_thenAllChildrenAreInParent() {
        Category root = Category.createRoot("패션");

        Category child1 = Category.createChild("상의", root);
        Category child2 = Category.createChild("하의", root);

        assertThat(root.getChildren()).containsExactly(child1, child2);
    }
}
