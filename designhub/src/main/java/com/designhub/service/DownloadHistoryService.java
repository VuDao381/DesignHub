package com.designhub.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.entity.Design;
import com.designhub.entity.DownloadHistory;
import com.designhub.entity.User;
import com.designhub.repository.DesignRepository;
import com.designhub.repository.DownloadHistoryRepository;
import com.designhub.repository.OrderRepository;
import com.designhub.security.CurrentUserService;

@Service
@Transactional
public class DownloadHistoryService {

    private final DownloadHistoryRepository downloadHistoryRepository;
    private final DesignRepository designRepository;
    private final OrderRepository orderRepository;
    private final CurrentUserService currentUserService;

    public DownloadHistoryService(
            DownloadHistoryRepository downloadHistoryRepository,
            DesignRepository designRepository,
            OrderRepository orderRepository,
            CurrentUserService currentUserService) {

        this.downloadHistoryRepository = downloadHistoryRepository;
        this.designRepository = designRepository;
        this.orderRepository = orderRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<DownloadHistory> getAllDownloadHistories() {
        return downloadHistoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<DownloadHistory> getDownloadHistoryById(Long id) {
        return downloadHistoryRepository.findById(id)
                .map(history -> {
                    currentUserService.checkOwnerOrAdmin(history.getUser().getId());
                    return history;
                });
    }

    @Transactional(readOnly = true)
    public List<DownloadHistory> getMyDownloadHistories() {
        User me = currentUserService.getCurrentUser();
        return downloadHistoryRepository.findByUserId(me.getId());
    }

    @Transactional(readOnly = true)
    public List<DownloadHistory> getDownloadHistoriesByUserId(Long userId) {
        currentUserService.checkOwnerOrAdmin(userId);
        return downloadHistoryRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<DownloadHistory> getDownloadHistoriesByDesignId(Long designId) {
        if (currentUserService.isAdmin()) {
            return downloadHistoryRepository.findByDesignId(designId);
        }
        User me = currentUserService.getCurrentUser();
        return downloadHistoryRepository.findByUserIdAndDesignId(me.getId(), designId)
                .map(List::of)
                .orElseGet(List::of);
    }

    @Transactional(readOnly = true)
    public Optional<DownloadHistory> getDownloadHistoryByUserAndDesign(
            Long userId,
            Long designId) {

        currentUserService.checkOwnerOrAdmin(userId);

        return downloadHistoryRepository
                .findByUserIdAndDesignId(userId, designId);
    }

    public DownloadHistory createDownloadHistory(Long designId) {
        User me = currentUserService.getCurrentUser();

        Design design = designRepository.findById(designId)
                .orElseThrow(() -> new IllegalArgumentException("Design không tồn tại"));

        boolean hasPurchased = orderRepository
                .existsByUserIdAndStatusAndOrderDetailsDesignId(
                        me.getId(),
                        "PAID",
                        designId);

        if (!hasPurchased && !currentUserService.isAdmin()) {
            throw new IllegalArgumentException("Bạn chưa mua thiết kế này");
        }

        Optional<DownloadHistory> existingHistory = downloadHistoryRepository
                .findByUserIdAndDesignId(me.getId(), designId);

        if (existingHistory.isPresent()) {
            DownloadHistory history = existingHistory.get();
            history.setDownloadedAt(LocalDateTime.now());
            return downloadHistoryRepository.save(history);
        }

        DownloadHistory history = new DownloadHistory();
        history.setId(null);
        history.setUser(me);
        history.setDesign(design);
        history.setDownloadedAt(LocalDateTime.now());

        return downloadHistoryRepository.save(history);
    }

    public boolean deleteDownloadHistory(Long id) {
        return downloadHistoryRepository.findById(id)
                .map(history -> {
                    currentUserService.checkOwnerOrAdmin(history.getUser().getId());
                    downloadHistoryRepository.delete(history);
                    return true;
                })
                .orElse(false);
    }

}
