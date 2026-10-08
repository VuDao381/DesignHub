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
import com.designhub.repository.UserRepository;

@Service
@Transactional
public class DownloadHistoryService {

    private final DownloadHistoryRepository downloadHistoryRepository;
    private final UserRepository userRepository;
    private final DesignRepository designRepository;
    private final OrderRepository orderRepository;

    public DownloadHistoryService(
            DownloadHistoryRepository downloadHistoryRepository,
            UserRepository userRepository,
            DesignRepository designRepository,
            OrderRepository orderRepository) {

        this.downloadHistoryRepository = downloadHistoryRepository;
        this.userRepository = userRepository;
        this.designRepository = designRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<DownloadHistory> getAllDownloadHistories() {
        return downloadHistoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<DownloadHistory> getDownloadHistoryById(Long id) {
        return downloadHistoryRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<DownloadHistory> getDownloadHistoriesByUserId(Long userId) {
        return downloadHistoryRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<DownloadHistory> getDownloadHistoriesByDesignId(Long designId) {
        return downloadHistoryRepository.findByDesignId(designId);
    }

    @Transactional(readOnly = true)
    public Optional<DownloadHistory> getDownloadHistoryByUserAndDesign(
            Long userId,
            Long designId) {

        return downloadHistoryRepository
                .findByUserIdAndDesignId(userId, designId);
    }

    public DownloadHistory createDownloadHistory(
            Long userId,
            Long designId) {

        User user = userRepository.findById(userId)
                .orElseThrow(()
                        -> new IllegalArgumentException(
                        "User không tồn tại"));

        Design design = designRepository.findById(designId)
                .orElseThrow(()
                        -> new IllegalArgumentException(
                        "Design không tồn tại"));

        boolean hasPurchased = orderRepository
                .existsByUserIdAndStatusAndOrderDetailsDesignId(
                        userId,
                        "PAID",
                        designId);

        if (!hasPurchased) {
            throw new IllegalArgumentException(
                    "User chưa mua Design này");
        }

        Optional<DownloadHistory> existingHistory
                = downloadHistoryRepository
                        .findByUserIdAndDesignId(userId, designId);

        if (existingHistory.isPresent()) {
            DownloadHistory history = existingHistory.get();
            history.setDownloadedAt(LocalDateTime.now());
            return downloadHistoryRepository.save(history);
        }

        DownloadHistory history = new DownloadHistory();
        history.setUser(user);
        history.setDesign(design);
        history.setDownloadedAt(LocalDateTime.now());

        return downloadHistoryRepository.save(history);
    }

    public boolean deleteDownloadHistory(Long id) {

        if (!downloadHistoryRepository.existsById(id)) {
            return false;
        }

        downloadHistoryRepository.deleteById(id);
        return true;
    }
}
