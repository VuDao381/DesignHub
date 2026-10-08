package com.designhub.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.designhub.entity.Review;
import com.designhub.service.ReviewService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ResponseEntity<List<Review>> getAllReviews() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Review> getReviewById(
            @PathVariable Long id) {

        return reviewService.getReviewById(id)
                .map(ResponseEntity::ok)
                .orElseGet(()
                        -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Review>> getReviewsByUserId(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                reviewService.getReviewsByUserId(userId));
    }

    @GetMapping("/design/{designId}")
    public ResponseEntity<List<Review>> getReviewsByDesignId(
            @PathVariable Long designId) {

        return ResponseEntity.ok(
                reviewService.getReviewsByDesignId(designId));
    }

    @GetMapping("/user/{userId}/design/{designId}")
    public ResponseEntity<Review> getReviewByUserAndDesign(
            @PathVariable Long userId,
            @PathVariable Long designId) {

        return reviewService
                .getReviewByUserAndDesign(userId, designId)
                .map(ResponseEntity::ok)
                .orElseGet(()
                        -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Review> createReview(
            @Valid @RequestBody Review review) {

        Review createdReview
                = reviewService.createReview(review);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdReview);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Review> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody Review review) {

        return reviewService
                .updateReview(id, review)
                .map(ResponseEntity::ok)
                .orElseGet(()
                        -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long id) {

        if (!reviewService.deleteReview(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}
