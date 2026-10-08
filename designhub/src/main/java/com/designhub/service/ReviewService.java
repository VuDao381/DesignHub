package com.designhub.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.entity.Design;
import com.designhub.entity.Review;
import com.designhub.entity.User;
import com.designhub.repository.DesignRepository;
import com.designhub.repository.ReviewRepository;
import com.designhub.repository.UserRepository;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final DesignRepository designRepository;

    public ReviewService(
            ReviewRepository reviewRepository,
            UserRepository userRepository,
            DesignRepository designRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.designRepository = designRepository;
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

        if (review.getUser() == null || review.getUser().getId() == null) {
            throw new IllegalArgumentException(
                    "Review phải gắn với một user hợp lệ");
        }

        if (review.getDesign() == null || review.getDesign().getId() == null) {
            throw new IllegalArgumentException(
                    "Review phải gắn với một design hợp lệ");
        }

        Long userId = review.getUser().getId();
        Long designId = review.getDesign().getId();

        User user = userRepository.findById(userId)
                .orElseThrow(()
                        -> new IllegalArgumentException("User không tồn tại"));

        Design design = designRepository.findById(designId)
                .orElseThrow(()
                        -> new IllegalArgumentException("Design không tồn tại"));

        if (reviewRepository.existsByUserIdAndDesignId(userId, designId)) {
            throw new IllegalArgumentException(
                    "User này đã đánh giá design");
        }

        if (review.getRating() < 1 || review.getRating() > 5) {
            throw new IllegalArgumentException(
                    "Đánh giá phải từ 1 đến 5");
        }

        review.setUser(user);
        review.setDesign(design);

        return reviewRepository.save(review);
    }

    public Optional<Review> updateReview(Long id, Review review) {

        return reviewRepository.findById(id)
                .map(existingReview -> {

                    if (review.getRating() < 1 || review.getRating() > 5) {
                        throw new IllegalArgumentException(
                                "Đánh giá phải từ 1 đến 5");
                    }

                    existingReview.setRating(review.getRating());
                    existingReview.setComment(review.getComment());

                    return reviewRepository.save(existingReview);
                });
    }

    public boolean deleteReview(Long id) {

        if (!reviewRepository.existsById(id)) {
            return false;
        }

        reviewRepository.deleteById(id);
        return true;
    }
}
