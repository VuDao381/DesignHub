package com.designhub.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.designhub.entity.Favorite;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUserId(Long userId);

    List<Favorite> findByDesignId(Long designId);

    Optional<Favorite> findByUserIdAndDesignId(
            Long userId,
            Long designId);

    boolean existsByUserIdAndDesignId(
            Long userId,
            Long designId);
}
