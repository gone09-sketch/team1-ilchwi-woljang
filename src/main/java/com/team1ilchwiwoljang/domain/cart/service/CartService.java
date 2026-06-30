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

    // 동시요청시 일어날 수 있는 예외로 DB 예외로 보내지않고, 서비스 예외로 바꿔주는 역할을함
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
     * 장바구니 주문 생성에 사용할 장바구니 상품 목록을 조회합니다.
     * 주문 생성은 명시적으로 선택된 장바구니 상품만 대상으로 하므로 cartIds가 비어 있으면 실패합니다.
     */
    @Transactional(readOnly = true)
    public List<Cart> getOrderCartItems(Long memberId, List<Long> cartIds) {
        if (cartIds == null || cartIds.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_CART_ORDER);
        }

        if (cartIds.stream().anyMatch(Objects::isNull)) {
            throw new BusinessException(ErrorCode.INVALID_CART_ITEM_ID);
        }

        List<Long> selectedCartIds = cartIds.stream()
                .distinct()
                .toList();

        return getSelectedCartItems(memberId, selectedCartIds);
    }

    /**
     * 주문서 미리보기에 사용할 장바구니 상품 목록을 조회합니다.
     * cartIds가 null이거나 비어 있으면 회원의 전체 장바구니를 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<Cart> getOrderPreviewCartItems(Long memberId, List<Long> cartIds) {
        if (cartIds == null || cartIds.isEmpty()) {
            return cartRepository.findAllByMemberIdWithProduct(memberId);
        }

        if (cartIds.stream().anyMatch(Objects::isNull)) {
            throw new BusinessException(ErrorCode.INVALID_CART_ITEM_ID);
        }

        List<Long> selectedCartIds = cartIds.stream()
                .distinct()
                .toList();

        return getSelectedCartItems(memberId, selectedCartIds);
    }

    @Transactional
    public void deleteOrderCartItems(List<Cart> cartItems) {
        cartRepository.deleteAll(cartItems);
    }

    private List<Cart> getSelectedCartItems(Long memberId, List<Long> selectedCartIds) {
        if (selectedCartIds.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        List<Cart> cartItems = cartRepository.findAllByMemberIdAndIdInWithProduct(memberId, selectedCartIds);

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
