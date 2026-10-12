package com.designhub.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.entity.Cart;
import com.designhub.entity.CartItem;
import com.designhub.entity.Design;
import com.designhub.entity.User;
import com.designhub.repository.CartItemRepository;
import com.designhub.repository.CartRepository;
import com.designhub.repository.DesignRepository;
import com.designhub.security.CurrentUserService;

@Service
@Transactional
public class CartItemService {

    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 100;

    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final DesignRepository designRepository;
    private final CurrentUserService currentUserService;

    public CartItemService(
            CartItemRepository cartItemRepository,
            CartRepository cartRepository,
            DesignRepository designRepository,
            CurrentUserService currentUserService) {

        this.cartItemRepository = cartItemRepository;
        this.cartRepository = cartRepository;
        this.designRepository = designRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<CartItem> getAllItems() {
        return cartItemRepository.findAll();
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
                designId);
    }

    public CartItem createItem(CartItem cartItem) {
        validateQuantity(cartItem.getQuantity());

        if (cartItem.getDesign() == null || cartItem.getDesign().getId() == null) {
            throw new IllegalArgumentException("CartItem phải gắn với một design hợp lệ");
        }

        User me = currentUserService.getCurrentUser();
        Cart cart = cartRepository.findByUserId(me.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(me);
                    return cartRepository.save(newCart);
                });

        Long designId = cartItem.getDesign().getId();
        Design design = designRepository.findById(designId)
                .orElseThrow(() -> new IllegalArgumentException("Design không tồn tại"));

        return cartItemRepository
                .findByCartIdAndDesignId(cart.getId(), designId)
                .map(existing -> {
                    int newQuantity = existing.getQuantity() + cartItem.getQuantity();
                    validateQuantity(newQuantity);
                    existing.setQuantity(newQuantity);
                    return cartItemRepository.save(existing);
                })
                .orElseGet(() -> {
                    cartItem.setCart(cart);
                    cartItem.setDesign(design);
                    return cartItemRepository.save(cartItem);
                });
    }

    public Optional<CartItem> updateItem(Long id, CartItem cartItem) {
        validateQuantity(cartItem.getQuantity());

        return cartItemRepository.findById(id)
                .map(existingItem -> {
                    currentUserService.checkOwnerOrAdmin(
                            existingItem.getCart().getUser().getId());

                    existingItem.setQuantity(cartItem.getQuantity());
                    return cartItemRepository.save(existingItem);
                });
    }

    public boolean deleteItem(Long id) {
        return cartItemRepository.findById(id)
                .map(item -> {
                    currentUserService.checkOwnerOrAdmin(
                            item.getCart().getUser().getId());
                    cartItemRepository.delete(item);
                    return true;
                })
                .orElse(false);
    }

    private void validateQuantity(int quantity) {

        if (quantity < MIN_QUANTITY
                || quantity > MAX_QUANTITY) {

            throw new IllegalArgumentException(
                    "Số lượng phải trong khoảng "
                            + MIN_QUANTITY
                            + " - "
                            + MAX_QUANTITY);
        }
    }
}
