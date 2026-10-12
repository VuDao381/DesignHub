package com.designhub.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.entity.Design;
import com.designhub.entity.Review;
import com.designhub.entity.User;
import com.designhub.repository.DesignRepository;
import com.designhub.repository.OrderRepository;
import com.designhub.repository.ReviewRepository;
import com.designhub.security.CurrentUserService;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final DesignRepository designRepository;
    private final OrderRepository orderRepository;
    private final CurrentUserService currentUserService;

    public ReviewService(
            ReviewRepository reviewRepository,
            DesignRepository designRepository,
            OrderRepository orderRepository,
            CurrentUserService currentUserService) {
        this.reviewRepository = reviewRepository;
        this.designRepository = designRepository;
        this.orderRepository = orderRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Review> getReviewById(Long id) {
        return reviewRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Review> getReviewsByUserId(Long userId) {
        return reviewRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Review> getReviewsByDesignId(Long designId) {
        return reviewRepository.findByDesignId(designId);
    }

    @Transactional(readOnly = true)
    public Optional<Review> getReviewByUserAndDesign(
            Long userId,
            Long designId) {
        return reviewRepository.findByUserIdAndDesignId(userId, designId);
    }

    public Review createReview(Review review) {
        if (review.getDesign() == null || review.getDesign().getId() == null) {
            throw new IllegalArgumentException("Review phải gắn với một design hợp lệ");
        }

        User me = currentUserService.getCurrentUser();
        Long designId = review.getDesign().getId();

        Design design = designRepository.findById(designId)
                .orElseThrow(() -> new IllegalArgumentException("Design không tồn tại"));

        boolean hasPurchased = orderRepository.existsByUserIdAndStatusAndOrderDetailsDesignId(
                me.getId(), "PAID", designId);
        if (!hasPurchased && !currentUserService.isAdmin()) {
            throw new IllegalArgumentException("Bạn chỉ có thể đánh giá thiết kế sau khi đã mua");
        }

        if (reviewRepository.existsByUserIdAndDesignId(me.getId(), designId)) {
            throw new IllegalArgumentException("Bạn đã đánh giá thiết kế này rồi");
        }

        if (review.getRating() < 1 || review.getRating() > 5) {
            throw new IllegalArgumentException("Đánh giá phải từ 1 đến 5 sao");
        }

        review.setUser(me);
        review.setDesign(design);

        return reviewRepository.save(review);
    }

    public Optional<Review> updateReview(Long id, Review review) {
        return reviewRepository.findById(id)
                .map(existingReview -> {
                    currentUserService.checkOwnerOrAdmin(existingReview.getUser().getId());

                    if (review.getRating() < 1 || review.getRating() > 5) {
                        throw new IllegalArgumentException("Đánh giá phải từ 1 đến 5 sao");
                    }

                    existingReview.setRating(review.getRating());
                    existingReview.setComment(review.getComment());

                    return reviewRepository.save(existingReview);
                });
    }

    public boolean deleteReview(Long id) {
        return reviewRepository.findById(id)
                .map(existingReview -> {
                    currentUserService.checkOwnerOrAdmin(existingReview.getUser().getId());
                    reviewRepository.delete(existingReview);
                    return true;
                })
                .orElse(false);
    }
}
