package com.designhub.service;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.entity.Category;
import com.designhub.entity.Design;
import com.designhub.entity.DesignFile;
import com.designhub.entity.DesignImage;
import com.designhub.entity.Designer;
import com.designhub.entity.User;
import com.designhub.exception.ResourceNotFoundException;
import com.designhub.repository.CartItemRepository;
import com.designhub.repository.CategoryRepository;
import com.designhub.repository.DesignRepository;
import com.designhub.repository.DesignerRepository;
import com.designhub.repository.OrderDetailRepository;
import com.designhub.security.CurrentUserService;
import com.designhub.specification.DesignSpecification;

@Service
@Transactional
public class DesignService {

    @Value("${file.upload-dir:uploads}")
    private String baseUploadDir = "uploads";

    private final DesignRepository designRepository;
    private final DesignerRepository designerRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CartItemRepository cartItemRepository;
    private final CurrentUserService currentUserService;
    private final CategoryRepository categoryRepository;

    public DesignService(
            DesignRepository designRepository,
            DesignerRepository designerRepository,
            OrderDetailRepository orderDetailRepository,
            CartItemRepository cartItemRepository,
            CurrentUserService currentUserService) {
        this(designRepository, designerRepository, orderDetailRepository, cartItemRepository, currentUserService, null);
    }

    @Autowired
    public DesignService(
            DesignRepository designRepository,
            DesignerRepository designerRepository,
            OrderDetailRepository orderDetailRepository,
            CartItemRepository cartItemRepository,
            CurrentUserService currentUserService,
            CategoryRepository categoryRepository) {
        this.designRepository = designRepository;
        this.designerRepository = designerRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.cartItemRepository = cartItemRepository;
        this.currentUserService = currentUserService;
        this.categoryRepository = categoryRepository;
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

        boolean isAdmin = false;
        boolean isOwnerFilteringOwnDesigns = false;
        try {
            isAdmin = currentUserService.isAdmin();
            if (!isAdmin && designerId != null) {
                User me = currentUserService.getCurrentUser();
                isOwnerFilteringOwnDesigns = designerRepository.findByUserId(me.getId())
                        .map(d -> d.getId().equals(designerId))
                        .orElse(false);
            }
        } catch (Exception ignored) {
        }

        String requiredStatus = (isAdmin || isOwnerFilteringOwnDesigns) ? null : "APPROVED";

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

        if (design.getCategory() != null && design.getCategory().getId() != null && categoryRepository != null) {
            Category category = categoryRepository.findById(design.getCategory().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại"));
            design.setCategory(category);
        }

        User me = currentUserService.getCurrentUser();
        if (!currentUserService.isAdmin()) {
            Designer designer = designerRepository.findByUserId(me.getId())
                    .orElseGet(() -> {
                        if (me.getRole() == com.designhub.entity.Role.DESIGNER) {
                            Designer newDesigner = new Designer();
                            newDesigner.setUser(me);
                            return designerRepository.save(newDesigner);
                        }
                        throw new AccessDeniedException(
                                "Tài khoản của bạn chưa được kích hoạt hồ sơ Designer");
                    });
            design.setDesigner(designer);
            design.setStatus("PENDING");
        } else {
            if (design.getDesigner() != null && design.getDesigner().getId() != null) {
                Designer designer = designerRepository.findById(design.getDesigner().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Designer không tồn tại"));
                design.setDesigner(designer);
            } else {
                Designer adminDesigner = designerRepository.findByUserId(me.getId())
                        .orElseGet(() -> {
                            Designer newDesigner = new Designer();
                            newDesigner.setUser(me);
                            newDesigner.setBio("System Administrator Designer Profile");
                            return designerRepository.save(newDesigner);
                        });
                design.setDesigner(adminDesigner);
            }
            if (design.getStatus() == null || design.getStatus().isBlank()) {
                design.setStatus("APPROVED");
            } else {
                design.setStatus(design.getStatus().trim().toUpperCase());
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

                    if (design.getCategory() != null && design.getCategory().getId() != null && categoryRepository != null) {
                        Category category = categoryRepository.findById(design.getCategory().getId())
                                .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại"));
                        existingDesign.setCategory(category);
                    } else if (design.getCategory() != null) {
                        existingDesign.setCategory(design.getCategory());
                    }

                    if (!currentUserService.isAdmin()) {
                        existingDesign.setStatus("PENDING");
                        existingDesign.setRejectReason(null);
                    } else if (design.getStatus() != null && !design.getStatus().isBlank()) {
                        existingDesign.setStatus(design.getStatus().trim().toUpperCase());
                    }

                    if (!"APPROVED".equalsIgnoreCase(existingDesign.getStatus())) {
                        cartItemRepository.deleteByDesignId(id);
                    }

                    if (currentUserService.isAdmin() && design.getDesigner() != null && design.getDesigner().getId() != null) {
                        Designer designer = designerRepository.findById(design.getDesigner().getId())
                                .orElseThrow(() -> new ResourceNotFoundException("Designer không tồn tại"));
                        existingDesign.setDesigner(designer);
                    }

                    return designRepository.save(existingDesign);
                });
    }

    @Transactional(readOnly = true)
    public Page<Design> getMyDesigns(Pageable pageable) {
        User me = currentUserService.getCurrentUser();
        return designerRepository.findByUserId(me.getId())
                .map(designer -> designRepository.findByDesignerId(designer.getId(), pageable))
                .orElseGet(() -> {
                    if (currentUserService.isAdmin() || me.getRole() == com.designhub.entity.Role.DESIGNER) {
                        return Page.empty(pageable);
                    }
                    throw new AccessDeniedException("Tài khoản của bạn chưa có hồ sơ Designer");
                });
    }

    public Optional<Design> updateDesignStatus(Long id, String status, String rejectReason) {
        if (!currentUserService.isAdmin()) {
            throw new AccessDeniedException("Chỉ Quản trị viên (ADMIN) mới có quyền phê duyệt/cập nhật trạng thái thiết kế");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Trạng thái thiết kế không được để trống");
        }

        String normalizedStatus = status.trim().toUpperCase();
        if (!java.util.Set.of("PENDING", "APPROVED", "REJECTED", "ARCHIVED").contains(normalizedStatus)) {
            throw new IllegalArgumentException(
                    "Trạng thái không hợp lệ. Chỉ chấp nhận: PENDING, APPROVED, REJECTED, ARCHIVED");
        }
        if ("REJECTED".equals(normalizedStatus) && (rejectReason == null || rejectReason.isBlank())) {
            throw new IllegalArgumentException("Vui lòng cung cấp lý do từ chối (rejectReason)");
        }

        return designRepository.findById(id)
                .map(existingDesign -> {
                    existingDesign.setStatus(normalizedStatus);
                    if ("REJECTED".equals(normalizedStatus)) {
                        existingDesign.setRejectReason(rejectReason.trim());
                    } else if ("APPROVED".equals(normalizedStatus)) {
                        existingDesign.setRejectReason(null);
                    }
                    if (!"APPROVED".equals(normalizedStatus)) {
                        cartItemRepository.deleteByDesignId(id);
                    }
                    return designRepository.save(existingDesign);
                });
    }

    public boolean deleteDesign(Long id) {
        return designRepository.findById(id)
                .map(design -> {
                    checkDesignOwnership(design);

                    cartItemRepository.deleteByDesignId(id);

                    if (orderDetailRepository.existsByDesignId(id)) {
                        design.setStatus("ARCHIVED");
                        designRepository.save(design);
                        return true;
                    }

                    deletePhysicalFilesOfDesign(design);
                    designRepository.delete(design);
                    return true;
                })
                .orElse(false);
    }

    private void deletePhysicalFilesOfDesign(Design design) {
        if (design.getImages() != null) {
            Path imagesBaseDir = Paths.get(baseUploadDir, "images").toAbsolutePath().normalize();
            for (DesignImage img : design.getImages()) {
                try {
                    if (img.getImageUrl() != null) {
                        String storedFileName = Paths.get(img.getImageUrl()).getFileName().toString();
                        Path filePath = imagesBaseDir.resolve(storedFileName).normalize();
                        if (filePath.startsWith(imagesBaseDir)) {
                            Files.deleteIfExists(filePath);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
        if (design.getFiles() != null) {
            Path filesBaseDir = Paths.get(baseUploadDir, "files").toAbsolutePath().normalize();
            for (DesignFile file : design.getFiles()) {
                try {
                    if (file.getFileUrl() != null) {
                        String storedFileName = Paths.get(file.getFileUrl()).getFileName().toString();
                        Path filePath = filesBaseDir.resolve(storedFileName).normalize();
                        if (filePath.startsWith(filesBaseDir)) {
                            Files.deleteIfExists(filePath);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
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
