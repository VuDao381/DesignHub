package com.designhub.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.entity.Design;
import com.designhub.entity.DesignImage;
import com.designhub.entity.User;
import com.designhub.repository.DesignImageRepository;
import com.designhub.repository.DesignRepository;
import com.designhub.security.CurrentUserService;

@Service
@Transactional
public class DesignImageService {

    private final DesignImageRepository designImageRepository;
    private final DesignRepository designRepository;
    private final CurrentUserService currentUserService;

    public DesignImageService(
            DesignImageRepository designImageRepository,
            DesignRepository designRepository,
            CurrentUserService currentUserService) {
        this.designImageRepository = designImageRepository;
        this.designRepository = designRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<DesignImage> getAllImages() {
        return designImageRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<DesignImage> getImagesByDesignId(Long designId) {
        return designImageRepository.findByDesignId(designId);
    }

    @Transactional(readOnly = true)
    public Optional<DesignImage> getImageById(Long id) {
        return designImageRepository.findById(id);
    }

    public DesignImage createImage(DesignImage designImage) {
        if (designImage.getDesign() == null || designImage.getDesign().getId() == null) {
            throw new IllegalArgumentException("Hình ảnh phải gắn với một thiết kế hợp lệ");
        }

        Design design = designRepository.findById(designImage.getDesign().getId())
                .orElseThrow(() -> new IllegalArgumentException("Thiết kế không tồn tại"));

        checkAuthorOrAdmin(design);

        designImage.setId(null);
        designImage.setDesign(design);
        return designImageRepository.save(designImage);
    }

    public Optional<DesignImage> updateImage(
            Long id,
            DesignImage designImage) {

        return designImageRepository.findById(id)
                .map(existingImage -> {
                    checkAuthorOrAdmin(existingImage.getDesign());

                    existingImage.setImageUrl(designImage.getImageUrl());
                    existingImage.setPrimary(designImage.isPrimary());

                    return designImageRepository.save(existingImage);
                });
    }

    public boolean deleteImage(Long id) {
        return designImageRepository.findById(id)
                .map(image -> {
                    checkAuthorOrAdmin(image.getDesign());
                    designImageRepository.delete(image);
                    return true;
                })
                .orElse(false);
    }

    private void checkAuthorOrAdmin(Design design) {
        if (currentUserService.isAdmin()) {
            return;
        }
        User me = currentUserService.getCurrentUser();
        if (design.getDesigner() == null
                || design.getDesigner().getUser() == null
                || !design.getDesigner().getUser().getId().equals(me.getId())) {
            throw new AccessDeniedException("Bạn không có quyền quản lý hình ảnh của thiết kế này");
        }
    }
}
