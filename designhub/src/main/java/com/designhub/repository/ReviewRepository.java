package com.designhub.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.designhub.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByUserId(Long userId);

    List<Review> findByDesignId(Long designId);

    Optional<Review> findByUserIdAndDesignId(
            Long userId,
            Long designId);

    boolean existsByUserIdAndDesignId(
            Long userId,
            Long designId);
}
