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
import com.designhub.repository.UserRepository;

@Service
@Transactional
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final DesignRepository designRepository;

    public FavoriteService(
            FavoriteRepository favoriteRepository,
            UserRepository userRepository,
            DesignRepository designRepository) {

        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.designRepository = designRepository;
    }

    @Transactional(readOnly = true)
    public List<Favorite> getAllFavorites() {
        return favoriteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Favorite> getFavoriteById(Long id) {
        return favoriteRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Favorite> getFavoritesByUserId(Long userId) {
        return favoriteRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Favorite> getFavoritesByDesignId(Long designId) {
        return favoriteRepository.findByDesignId(designId);
    }

    @Transactional(readOnly = true)
    public Optional<Favorite> getFavoriteByUserAndDesign(
            Long userId,
            Long designId) {

        return favoriteRepository.findByUserIdAndDesignId(
                userId,
                designId
        );
    }

    public Favorite createFavorite(Favorite favorite) {

        if (favorite.getUser() == null
                || favorite.getUser().getId() == null) {

            throw new IllegalArgumentException(
                    "Favorite phải gắn với một user hợp lệ");
        }

        if (favorite.getDesign() == null
                || favorite.getDesign().getId() == null) {

            throw new IllegalArgumentException(
                    "Favorite phải gắn với một design hợp lệ");
        }

        Long userId = favorite.getUser().getId();
        Long designId = favorite.getDesign().getId();

        User user = userRepository.findById(userId)
                .orElseThrow(()
                        -> new IllegalArgumentException(
                        "User không tồn tại"));

        Design design = designRepository.findById(designId)
                .orElseThrow(()
                        -> new IllegalArgumentException(
                        "Design không tồn tại"));

        if (favoriteRepository.existsByUserIdAndDesignId(
                userId,
                designId)) {

            throw new IllegalArgumentException(
                    "Design này đã được yêu thích");
        }

        favorite.setUser(user);
        favorite.setDesign(design);

        return favoriteRepository.save(favorite);
    }

    public boolean deleteFavorite(Long id) {

        if (!favoriteRepository.existsById(id)) {
            return false;
        }

        favoriteRepository.deleteById(id);
        return true;
    }
}
