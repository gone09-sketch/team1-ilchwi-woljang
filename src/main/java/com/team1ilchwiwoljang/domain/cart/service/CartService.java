package com.team1ilchwiwoljang.domain.cart.service;

import com.team1ilchwiwoljang.domain.cart.dto.response.CartItemResponse;
import com.team1ilchwiwoljang.domain.cart.dto.response.CartResponse;
import com.team1ilchwiwoljang.domain.cart.entity.Cart;
import com.team1ilchwiwoljang.domain.cart.repository.CartRepository;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;

    @Transactional(readOnly = true)
    public CartResponse getCart(Long memberId) {
        List<CartItemResponse> items = cartRepository.findAllByMemberIdWithProduct(memberId).stream()
                .map(this::toItemResponse)
                .toList();
        long cartTotalPrice = items.stream()
                .mapToLong(CartItemResponse::itemTotalPrice)
                .sum();

        return new CartResponse(items, cartTotalPrice);
    }

    private CartItemResponse toItemResponse(Cart cart) {
        Product product = cart.getProduct();
        long itemTotalPrice = (long) product.getPrice() * cart.getQuantity();
        boolean orderable = product.isOnSale() && product.getStock() >= cart.getQuantity();

        return new CartItemResponse(
                cart.getId(),
                product.getId(),
                product.getName(),
                product.getPrice(),
                cart.getQuantity(),
                itemTotalPrice,
                product.getStock(),
                product.getStatus(),
                orderable
        );
    }
}
