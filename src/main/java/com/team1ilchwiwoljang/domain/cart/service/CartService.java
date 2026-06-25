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
import java.util.Objects;
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
        throw new BusinessException(ErrorCode.CART_ITEM_QUANTITY_EXCEEDED);
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

    /**
     * 주문서 미리보기에 사용할 장바구니 상품 목록을 조회합니다.
     * cartIds가 null이거나 비어 있으면 회원의 전체 장바구니를 조회합니다.
     * cartIds가 있으면 선택된 장바구니 상품만 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<Cart> getOrderPreviewCartItems(Long memberId, List<Long> cartIds) {
        if (cartIds == null || cartIds.isEmpty()) {
            return cartRepository.findAllByMemberIdWithProduct(memberId);
        }

        // null ID는 어떤 장바구니 상품을 뜻하는지 알 수 없으므로 잘못된 요청으로 봅니다.
        if (cartIds.stream().anyMatch(Objects::isNull)) {
            throw new BusinessException(ErrorCode.INVALID_CART_ITEM_ID);
        }

        // 같은 cartId가 중복으로 들어와도 한 번만 조회되도록 정리합니다.
        List<Long> selectedCartIds = cartIds.stream()
                .distinct()
                .toList();

        if (selectedCartIds.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        List<Cart> cartItems = cartRepository.findAllByMemberIdAndIdInWithProduct(memberId, selectedCartIds);

        // 다른 회원의 장바구니 ID나 존재하지 않는 ID가 섞이면 조회 개수가 줄어듭니다.
        if (cartItems.size() != selectedCartIds.size()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        return cartItems;
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
