package com.designhub.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.designhub.entity.DownloadHistory;

public interface DownloadHistoryRepository
        extends JpaRepository<DownloadHistory, Long> {

    List<DownloadHistory> findByUserId(Long userId);

    List<DownloadHistory> findByDesignId(Long designId);

    Optional<DownloadHistory> findByUserIdAndDesignId(
            Long userId,
            Long designId);

    boolean existsByUserIdAndDesignId(
            Long userId,
            Long designId);
}
