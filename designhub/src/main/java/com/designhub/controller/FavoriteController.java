package com.designhub.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.designhub.entity.Favorite;
import com.designhub.service.FavoriteService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public ResponseEntity<List<Favorite>> getAllFavorites() {
        return ResponseEntity.ok(
                favoriteService.getAllFavorites()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Favorite> getFavoriteById(
            @PathVariable Long id) {

        return favoriteService.getFavoriteById(id)
                .map(ResponseEntity::ok)
                .orElseGet(()
                        -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Favorite>> getFavoritesByUserId(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                favoriteService.getFavoritesByUserId(userId)
        );
    }

    @GetMapping("/design/{designId}")
    public ResponseEntity<List<Favorite>> getFavoritesByDesignId(
            @PathVariable Long designId) {

        return ResponseEntity.ok(
                favoriteService.getFavoritesByDesignId(designId)
        );
    }

    @GetMapping("/user/{userId}/design/{designId}")
    public ResponseEntity<Favorite> getFavoriteByUserAndDesign(
            @PathVariable Long userId,
            @PathVariable Long designId) {

        return favoriteService
                .getFavoriteByUserAndDesign(userId, designId)
                .map(ResponseEntity::ok)
                .orElseGet(()
                        -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Favorite> createFavorite(
            @Valid @RequestBody Favorite favorite) {

        Favorite created
                = favoriteService.createFavorite(favorite);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFavorite(
            @PathVariable Long id) {

        if (!favoriteService.deleteFavorite(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}
