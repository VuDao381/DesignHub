package com.designhub.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.entity.Design;
import com.designhub.entity.Favorite;
import com.designhub.entity.User;
import com.designhub.repository.DesignRepository;
import com.designhub.repository.FavoriteRepository;
import com.designhub.security.CurrentUserService;

@Service
@Transactional
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final DesignRepository designRepository;
    private final CurrentUserService currentUserService;

    public FavoriteService(
            FavoriteRepository favoriteRepository,
            DesignRepository designRepository,
            CurrentUserService currentUserService) {
        this.favoriteRepository = favoriteRepository;
        this.designRepository = designRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<Favorite> getAllFavorites() {
        return favoriteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Favorite> getMyFavorites() {
        User me = currentUserService.getCurrentUser();
        return favoriteRepository.findByUserId(me.getId());
    }

    @Transactional(readOnly = true)
    public Optional<Favorite> getFavoriteById(Long id) {
        return favoriteRepository.findById(id)
                .map(favorite -> {
                    currentUserService.checkOwnerOrAdmin(favorite.getUser().getId());
                    return favorite;
                });
    }

    @Transactional(readOnly = true)
    public List<Favorite> getFavoritesByUserId(Long userId) {
        currentUserService.checkOwnerOrAdmin(userId);
        return favoriteRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Favorite> getFavoritesByDesignId(Long designId) {
        if (currentUserService.isAdmin()) {
            return favoriteRepository.findByDesignId(designId);
        }
        User me = currentUserService.getCurrentUser();
        return favoriteRepository.findByUserIdAndDesignId(me.getId(), designId)
                .map(List::of)
                .orElseGet(List::of);
    }

    @Transactional(readOnly = true)
    public Optional<Favorite> getFavoriteByUserAndDesign(
            Long userId,
            Long designId) {

        currentUserService.checkOwnerOrAdmin(userId);

        return favoriteRepository.findByUserIdAndDesignId(
                userId,
                designId);
    }

    public Favorite createFavorite(Favorite favorite) {
        favorite.setId(null);

        if (favorite.getDesign() == null || favorite.getDesign().getId() == null) {
            throw new IllegalArgumentException("Favorite phải gắn với một design hợp lệ");
        }
        User me = currentUserService.getCurrentUser();
        Long designId = favorite.getDesign().getId();
        Design design = designRepository.findById(designId)
                .orElseThrow(() -> new IllegalArgumentException("Design không tồn tại"));
        if (favoriteRepository.existsByUserIdAndDesignId(me.getId(), designId)) {
            throw new IllegalArgumentException("Thiết kế đã có trong danh sách yêu thích");
        }
        favorite.setUser(me);
        favorite.setDesign(design);
        return favoriteRepository.save(favorite);
    }

    public boolean deleteFavorite(Long id) {
        return favoriteRepository.findById(id)
                .map(favorite -> {
                    currentUserService.checkOwnerOrAdmin(favorite.getUser().getId());
                    favoriteRepository.delete(favorite);
                    return true;
                })
                .orElse(false);
    }
}
