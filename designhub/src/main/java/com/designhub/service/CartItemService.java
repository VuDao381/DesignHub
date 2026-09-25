package com.designhub.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.entity.CartItem;
import com.designhub.repository.CartItemRepository;

@Service
@Transactional
public class CartItemService {

    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 100;

    private final CartItemRepository cartItemRepository;

    public CartItemService(CartItemRepository cartItemRepository) {
        this.cartItemRepository = cartItemRepository;
    }

    @Transactional(readOnly = true)
    public List<CartItem> getItemsByCartId(Long cartId) {
        return cartItemRepository.findByCartId(cartId);
    }

    @Transactional(readOnly = true)
    public Optional<CartItem> getItemById(Long id) {
        return cartItemRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<CartItem> getItemByCartAndDesign(
            Long cartId,
            Long designId) {

        return cartItemRepository.findByCartIdAndDesignId(
                cartId,
                designId
        );
    }

    public CartItem createItem(CartItem cartItem) {
        validateQuantity(cartItem.getQuantity());

        if (cartItem.getCart() == null || cartItem.getCart().getId() == null) {
            throw new IllegalArgumentException("CartItem phải gắn với một cart hợp lệ");
        }
        if (cartItem.getDesign() == null || cartItem.getDesign().getId() == null) {
            throw new IllegalArgumentException("CartItem phải gắn với một design hợp lệ");
        }

        return cartItemRepository
                .findByCartIdAndDesignId(
                        cartItem.getCart().getId(),
                        cartItem.getDesign().getId())
                .map(existing -> {
                    int newQuantity = existing.getQuantity() + cartItem.getQuantity();
                    validateQuantity(newQuantity);
                    existing.setQuantity(newQuantity);
                    return cartItemRepository.save(existing);
                })
                .orElseGet(() -> cartItemRepository.save(cartItem));
    }

    public Optional<CartItem> updateItem(
            Long id,
            CartItem cartItem) {

        validateQuantity(cartItem.getQuantity());

        return cartItemRepository.findById(id)
                .map(existingItem -> {
                    existingItem.setQuantity(cartItem.getQuantity());

                    return cartItemRepository.save(existingItem);
                });
    }

    public boolean deleteItem(Long id) {
        if (!cartItemRepository.existsById(id)) {
            return false;
        }

        cartItemRepository.deleteById(id);
        return true;
    }

    private void validateQuantity(int quantity) {
        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException(
                    "Số lượng phải trong khoảng " + MIN_QUANTITY + " - " + MAX_QUANTITY);
        }
    }
}
