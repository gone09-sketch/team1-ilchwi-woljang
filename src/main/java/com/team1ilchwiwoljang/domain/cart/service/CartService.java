package com.team1ilchwiwoljang.domain.cart.service;

import com.team1ilchwiwoljang.domain.cart.dto.CartCreateRequest;
import com.team1ilchwiwoljang.domain.cart.dto.CartResponse;
import com.team1ilchwiwoljang.domain.cart.entity.Cart;
import com.team1ilchwiwoljang.domain.cart.repository.CartRepository;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final MemberService memberService;
    private final ProductService productService;

    @Transactional
    public CartResponse addCartItem(
            Long memberId, CartCreateRequest request){
        Member member = memberService.getMember(memberId);
        Product product = productService.getProduct(request.productId());

        Cart cart = cartRepository.findByMemberIdAndProductId(memberId, product.getId())
                .orElseGet(() -> cartRepository.save(Cart.create(member, product, 0)));

        cart.increaseQuantity(request.quantity());

        return CartResponse.from(cart);
    }

}
