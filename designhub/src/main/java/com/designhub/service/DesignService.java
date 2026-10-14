package com.designhub.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.entity.Design;
import com.designhub.entity.Designer;
import com.designhub.entity.User;
import com.designhub.repository.DesignRepository;
import com.designhub.repository.DesignerRepository;
import com.designhub.security.CurrentUserService;

@Service
@Transactional
public class DesignService {

    private final DesignRepository designRepository;
    private final DesignerRepository designerRepository;
    private final CurrentUserService currentUserService;

    public DesignService(DesignRepository designRepository, DesignerRepository designerRepository,
            CurrentUserService currentUserService) {
        this.designRepository = designRepository;
        this.designerRepository = designerRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public Page<Design> getDesigns(String keyword, Pageable pageable) {
        boolean isAdmin = false;
        try {
            isAdmin = currentUserService.isAdmin();
        } catch (Exception ignored) {
        }

        if (isAdmin) {
            if (keyword == null || keyword.trim().isEmpty()) {
                return designRepository.findAll(pageable);
            }
            return designRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable);
        }

        if (keyword == null || keyword.trim().isEmpty()) {
            return designRepository.findByStatus("APPROVED", pageable);
        }
        return designRepository.findByStatusAndNameContainingIgnoreCase(
                "APPROVED", keyword.trim(), pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Design> getDesignById(Long id) {
        return designRepository.findById(id)
                .filter(design -> {
                    if ("APPROVED".equalsIgnoreCase(design.getStatus())) {
                        return true;
                    }
                    try {
                        if (currentUserService.isAdmin()) {
                            return true;
                        }
                        User me = currentUserService.getCurrentUser();
                        return design.getDesigner() != null
                                && design.getDesigner().getUser() != null
                                && design.getDesigner().getUser().getId().equals(me.getId());
                    } catch (Exception ignored) {
                        return false;
                    }
                });
    }

    public Design createDesign(Design design) {
        design.setId(null);

        User me = currentUserService.getCurrentUser();
        if (!currentUserService.isAdmin()) {
            Designer designer = designerRepository.findByUserId(me.getId())
                    .orElseThrow(() -> new AccessDeniedException(
                            "Tài khoản của bạn chưa được kích hoạt hồ sơ Designer"));
            design.setDesigner(designer);
            design.setStatus("PENDING");
        } else {
            if (design.getStatus() == null) {
                design.setStatus("APPROVED");
            }
        }
        return designRepository.save(design);
    }

    public Optional<Design> updateDesign(Long id, Design design) {
        return designRepository.findById(id)
                .map(existingDesign -> {
                    checkDesignOwnership(existingDesign);
                    existingDesign.setName(design.getName());
                    existingDesign.setDescription(design.getDescription());
                    existingDesign.setPrice(design.getPrice());
                    existingDesign.setCategory(design.getCategory());

                    if (currentUserService.isAdmin() && design.getStatus() != null) {
                        existingDesign.setStatus(design.getStatus());
                    }

                    if (currentUserService.isAdmin() && design.getDesigner() != null) {
                        existingDesign.setDesigner(design.getDesigner());
                    }

                    return designRepository.save(existingDesign);
                });
    }

    public boolean deleteDesign(Long id) {
        return designRepository.findById(id)
                .map(design -> {
                    checkDesignOwnership(design);
                    designRepository.delete(design);
                    return true;
                })
                .orElse(false);
    }

    private void checkDesignOwnership(Design design) {
        if (currentUserService.isAdmin()) {
            return;
        }
        User me = currentUserService.getCurrentUser();
        if (design.getDesigner() == null
                || design.getDesigner().getUser() == null
                || !design.getDesigner().getUser().getId().equals(me.getId())) {
            throw new AccessDeniedException("Bạn không phải tác giả của thiết kế này");
        }
    }
}
