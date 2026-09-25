package com.designhub.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.entity.Cart;
import com.designhub.repository.CartRepository;

@Service
@Transactional
public class CartService {

    private final CartRepository cartRepository;

    public CartService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Cart> getCartById(Long id) {
        return cartRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Cart> getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId);
    }

    public Cart getOrCreateCartForUser(com.designhub.entity.User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });
    }

    public Cart createCart(Cart cart) {
        if (cart.getUser() == null || cart.getUser().getId() == null) {
            throw new IllegalArgumentException("Cart phải gắn với một user hợp lệ");
        }

        cartRepository.findByUserId(cart.getUser().getId())
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "User đã có cart, không thể tạo mới");
                });

        return cartRepository.save(cart);
    }

    public boolean deleteCart(Long id) {
        if (!cartRepository.existsById(id)) {
            return false;
        }

        cartRepository.deleteById(id);
        return true;
    }
}
