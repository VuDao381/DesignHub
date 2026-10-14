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
import com.designhub.repository.OrderRepository;
import com.designhub.security.CurrentUserService;

@Service
@Transactional
public class CartItemService {

    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final DesignRepository designRepository;
    private final OrderRepository orderRepository;
    private final CurrentUserService currentUserService;

    public CartItemService(
            CartItemRepository cartItemRepository,
            CartRepository cartRepository,
            DesignRepository designRepository,
            OrderRepository orderRepository,
            CurrentUserService currentUserService) {

        this.cartItemRepository = cartItemRepository;
        this.cartRepository = cartRepository;
        this.designRepository = designRepository;
        this.orderRepository = orderRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<CartItem> getAllItems() {
        return cartItemRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<CartItem> getItemsByCartId(Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giỏ hàng"));
        currentUserService.checkOwnerOrAdmin(cart.getUser().getId());
        return cartItemRepository.findByCartId(cartId);
    }

    @Transactional(readOnly = true)
    public Optional<CartItem> getItemById(Long id) {
        return cartItemRepository.findById(id)
                .map(item -> {
                    currentUserService.checkOwnerOrAdmin(item.getCart().getUser().getId());
                    return item;
                });
    }

    @Transactional(readOnly = true)
    public Optional<CartItem> getItemByCartAndDesign(
            Long cartId,
            Long designId) {

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giỏ hàng"));
        currentUserService.checkOwnerOrAdmin(cart.getUser().getId());

        return cartItemRepository.findByCartIdAndDesignId(
                cartId,
                designId);
    }

    public CartItem createItem(CartItem cartItem) {
        if (cartItem.getDesign() == null || cartItem.getDesign().getId() == null) {
            throw new IllegalArgumentException("CartItem phải gắn với một design hợp lệ");
        }

        User me = currentUserService.getCurrentUser();
        Long designId = cartItem.getDesign().getId();
        Design design = designRepository.findById(designId)
                .orElseThrow(() -> new IllegalArgumentException("Design không tồn tại"));

        if (!"APPROVED".equalsIgnoreCase(design.getStatus())) {
            throw new IllegalArgumentException("Thiết kế chưa được phê duyệt để bán");
        }

        if (design.getDesigner() != null
                && design.getDesigner().getUser() != null
                && design.getDesigner().getUser().getId().equals(me.getId())) {
            throw new IllegalArgumentException("Bạn không thể mua thiết kế của chính mình");
        }

        if (orderRepository.existsByUserIdAndStatusAndOrderDetailsDesignId(me.getId(), "PAID", designId)) {
            throw new IllegalArgumentException("Bạn đã mua thiết kế này rồi");
        }

        Cart cart = cartRepository.findByUserId(me.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(me);
                    return cartRepository.save(newCart);
                });

        if (cartItemRepository.findByCartIdAndDesignId(cart.getId(), designId).isPresent()) {
            throw new IllegalArgumentException("Thiết kế này đã có trong giỏ hàng");
        }

        cartItem.setId(null);
        cartItem.setCart(cart);
        cartItem.setDesign(design);
        cartItem.setQuantity(1);

        return cartItemRepository.save(cartItem);
    }

    public Optional<CartItem> updateItem(Long id, CartItem cartItem) {
        return cartItemRepository.findById(id)
                .map(existingItem -> {
                    currentUserService.checkOwnerOrAdmin(
                            existingItem.getCart().getUser().getId());

                    existingItem.setQuantity(1);
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
}
