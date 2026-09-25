package com.designhub.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.designhub.entity.CartItem;
import com.designhub.service.CartItemService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cart-items")
public class CartItemController {

    private final CartItemService cartItemService;

    public CartItemController(CartItemService cartItemService) {
        this.cartItemService = cartItemService;
    }

    @GetMapping("/cart/{cartId}")
    public ResponseEntity<List<CartItem>> getItemsByCartId(
            @PathVariable Long cartId) {

        return ResponseEntity.ok(
                cartItemService.getItemsByCartId(cartId)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<CartItem> getItemById(
            @PathVariable Long id) {

        return cartItemService.getItemById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CartItem> createItem(
            @Valid @RequestBody CartItem cartItem) {

        return ResponseEntity.ok(
                cartItemService.createItem(cartItem)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<CartItem> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody CartItem cartItem) {

        return cartItemService.updateItem(id, cartItem)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long id) {

        if (!cartItemService.deleteItem(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}
