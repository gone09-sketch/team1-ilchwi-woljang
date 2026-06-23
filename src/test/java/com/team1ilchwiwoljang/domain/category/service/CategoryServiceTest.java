package com.team1ilchwiwoljang.domain.category.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.team1ilchwiwoljang.domain.category.dto.response.CategoryResponse;
import com.team1ilchwiwoljang.domain.category.entity.Category;
import com.team1ilchwiwoljang.domain.category.repository.CategoryRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    @DisplayName("카테고리가 없으면 빈 리스트를 반환한다")
    void givenNoCategories_whenGetCategories_thenReturnsEmptyList() {
        given(categoryRepository.findAllRootWithChildren()).willReturn(List.of());

        List<CategoryResponse> result = categoryService.getCategories();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("자식이 없는 루트 카테고리는 children이 빈 리스트인 응답을 반환한다")
    void givenRootCategoriesWithNoChildren_whenGetCategories_thenReturnsResponseWithEmptyChildren() {
        Category root1 = Category.createRoot("패션");
        Category root2 = Category.createRoot("전자기기");
        given(categoryRepository.findAllRootWithChildren()).willReturn(List.of(root1, root2));

        List<CategoryResponse> result = categoryService.getCategories();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("패션");
        assertThat(result.get(0).children()).isEmpty();
        assertThat(result.get(1).name()).isEqualTo("전자기기");
        assertThat(result.get(1).children()).isEmpty();
    }

    @Test
    @DisplayName("루트 카테고리에 자식이 있으면 자식까지 포함한 응답을 반환한다")
    void givenRootWithChildren_whenGetCategories_thenReturnsNestedResponse() {
        Category root = Category.createRoot("패션");
        Category child1 = Category.createChild("상의", root);
        Category child2 = Category.createChild("하의", root);
        root.getChildren().add(child1);
        root.getChildren().add(child2);
        given(categoryRepository.findAllRootWithChildren()).willReturn(List.of(root));

        List<CategoryResponse> result = categoryService.getCategories();

        assertThat(result).hasSize(1);
        CategoryResponse rootResponse = result.get(0);
        assertThat(rootResponse.name()).isEqualTo("패션");
        assertThat(rootResponse.children()).hasSize(2);
        assertThat(rootResponse.children())
                .extracting(CategoryResponse::name)
                .containsExactly("상의", "하의");
    }
}
