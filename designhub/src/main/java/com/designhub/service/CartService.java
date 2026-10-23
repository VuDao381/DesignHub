package com.designhub.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.entity.Cart;
import com.designhub.entity.User;
import com.designhub.repository.CartRepository;
import com.designhub.security.CurrentUserService;

@Service
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CurrentUserService currentUserService;

    public CartService(CartRepository cartRepository, CurrentUserService currentUserService) {
        this.cartRepository = cartRepository;
        this.currentUserService = currentUserService;
    }

    public Cart getMyCart() {
        User me = currentUserService.getCurrentUser();
        return cartRepository.findByUserId(me.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(me);
                    return cartRepository.save(newCart);
                });
    }

    @Transactional(readOnly = true)
    public Optional<Cart> getCartById(Long id) {
        return cartRepository.findById(id)
                .map(cart -> {
                    currentUserService.checkOwnerOrAdmin(cart.getUser().getId());
                    return cart;
                });
    }

    @Transactional(readOnly = true)
    public Optional<Cart> getCartByUserId(Long userId) {
        currentUserService.checkOwnerOrAdmin(userId);
        return cartRepository.findByUserId(userId);
    }

    public Cart createCart(Cart cart) {
        if (!currentUserService.isAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Quản trị viên (ADMIN) mới có quyền tạo giỏ hàng thủ công");
        }
        if (cart.getUser() == null || cart.getUser().getId() == null) {
            throw new IllegalArgumentException("Cart phải gắn với một user hợp lệ");
        }
        cart.setId(null);
        cartRepository.findByUserId(cart.getUser().getId())
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "User đã có cart, không thể tạo mới");
                });
        return cartRepository.save(cart);
    }

    public boolean deleteCart(Long id) {
        return cartRepository.findById(id)
                .map(cart -> {
                    currentUserService.checkOwnerOrAdmin(cart.getUser().getId());
                    cartRepository.delete(cart);
                    return true;
                })
                .orElse(false);
    }

    public Cart getOrCreateCartForUser(com.designhub.entity.User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });
    }

}
