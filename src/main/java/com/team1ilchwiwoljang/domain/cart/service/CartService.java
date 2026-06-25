package com.team1ilchwiwoljang.domain.cart.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.cart.dto.CartCreateRequest;
import com.team1ilchwiwoljang.domain.cart.dto.response.CartAddResponse;
import com.team1ilchwiwoljang.domain.cart.dto.response.CartItemResponse;
import com.team1ilchwiwoljang.domain.cart.dto.response.CartResponse;
import com.team1ilchwiwoljang.domain.cart.entity.Cart;
import com.team1ilchwiwoljang.domain.cart.repository.CartRepository;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.service.ProductService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final MemberService memberService;
    private final ProductService productService;

    @Retryable(
            retryFor = DataIntegrityViolationException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 50)
    )
    @Transactional
    public CartAddResponse addCartItem(
            Long memberId, CartCreateRequest request) {
        Member member = memberService.getMember(memberId);
        Product product = productService.getProduct(request.productId());

        Optional<Cart> optionalCart = cartRepository.findByMemberIdAndProductId(memberId, product.getId());
        Cart cart;
        if (optionalCart.isPresent()) {
            cart = optionalCart.get();
            int newQuantity = cart.getQuantity() + request.quantity();
            if (newQuantity > product.getStock()) {
                throw new BusinessException(ErrorCode.CART_ITEM_QUANTITY_EXCEEDED);
            }
            cart.increaseQuantity(request.quantity());
        } else {
            if (request.quantity() > product.getStock()) {
                throw new BusinessException(ErrorCode.CART_ITEM_QUANTITY_EXCEEDED);
            }
            cart = Cart.create(member, product, request.quantity());
            cart = cartRepository.save(cart);
        }

        return CartAddResponse.from(cart);
    }

    @Recover
    public CartAddResponse recoverAddCartItem(
            DataIntegrityViolationException e, Long memberId, CartCreateRequest request) {
        throw new BusinessException(ErrorCode.DUPLICATE_CART_ITEM);
    }

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
