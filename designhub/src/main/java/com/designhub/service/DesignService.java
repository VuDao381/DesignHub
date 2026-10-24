package com.designhub.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.entity.Design;
import com.designhub.entity.Designer;
import com.designhub.entity.User;
import com.designhub.repository.DesignRepository;
import com.designhub.repository.DesignerRepository;
import com.designhub.security.CurrentUserService;
import com.designhub.specification.DesignSpecification;

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
        return searchAndFilterDesigns(keyword, null, null, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Design> searchAndFilterDesigns(
            String keyword,
            Long categoryId,
            Long designerId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String sortBy,
            Pageable pageable) {

        boolean canViewAllStatuses = false;
        try {
            if (currentUserService.isAdmin()) {
                canViewAllStatuses = true;
            } else if (designerId != null) {
                User me = currentUserService.getCurrentUser();
                Optional<Designer> myDesigner = designerRepository.findByUserId(me.getId());
                if (myDesigner.isPresent() && myDesigner.get().getId().equals(designerId)) {
                    canViewAllStatuses = true;
                }
            }
        } catch (Exception ignored) {
        }

        String requiredStatus = canViewAllStatuses ? null : "APPROVED";

        Sort sort = resolveSort(sortBy);
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort);

        Specification<Design> spec = DesignSpecification.filterDesigns(
                keyword,
                categoryId,
                designerId,
                minPrice,
                maxPrice,
                requiredStatus);

        return designRepository.findAll(spec, sortedPageable);
    }

    @Transactional(readOnly = true)
    public Page<Design> getMyDesigns(
            String keyword,
            Long categoryId,
            String sortBy,
            Pageable pageable) {

        User me = currentUserService.getCurrentUser();
        Designer designer = designerRepository.findByUserId(me.getId())
                .orElseThrow(() -> new AccessDeniedException("Tài khoản chưa có hồ sơ Designer"));

        Sort sort = resolveSort(sortBy);
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort);

        Specification<Design> spec = DesignSpecification.filterDesigns(
                keyword,
                categoryId,
                designer.getId(),
                null,
                null,
                null);

        return designRepository.findAll(spec, sortedPageable);
    }

    private Sort resolveSort(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return switch (sortBy.trim().toLowerCase()) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "name_asc" -> Sort.by(Sort.Direction.ASC, "name");
            case "createdat_asc", "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
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
