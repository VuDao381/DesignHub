package com.designhub.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.entity.Design;
import com.designhub.entity.DesignFile;
import com.designhub.entity.User;
import com.designhub.repository.DesignFileRepository;
import com.designhub.repository.DesignRepository;
import com.designhub.repository.OrderRepository;
import com.designhub.security.CurrentUserService;

@Service
public class DesignFileService {

    private final DesignFileRepository designFileRepository;
    private final DesignRepository designRepository;
    private final OrderRepository orderRepository;
    private final CurrentUserService currentUserService;

    public DesignFileService(DesignFileRepository designFileRepository, DesignRepository designRepository,
            OrderRepository orderRepository, CurrentUserService currentUserService) {
        this.designFileRepository = designFileRepository;
        this.designRepository = designRepository;
        this.orderRepository = orderRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<DesignFile> getAllFiles() {
        return designFileRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<DesignFile> getFilesByDesignId(Long designId) {
        checkAccessPermission(designId);
        return designFileRepository.findByDesignId(designId);
    }

    @Transactional(readOnly = true)
    public Optional<DesignFile> getFileById(Long id) {
        return designFileRepository.findById(id)
                .map(file -> {
                    checkAccessPermission(file.getDesign().getId());
                    return file;
                });
    }

    public DesignFile createFile(DesignFile designFile) {
        if (designFile.getDesign() == null || designFile.getDesign().getId() == null) {
            throw new IllegalArgumentException("File phải gắn với một Design hợp lệ");
        }
        Design design = designRepository.findById(designFile.getDesign().getId())
                .orElseThrow(() -> new IllegalArgumentException("Design không tồn tại"));
        checkAuthorOrAdmin(design);
        designFile.setDesign(design);
        return designFileRepository.save(designFile);
    }

    public Optional<DesignFile> updateFile(
            Long id,
            DesignFile designFile) {

        return designFileRepository.findById(id)
                .map(existingFile -> {
                    checkAuthorOrAdmin(existingFile.getDesign());
                    existingFile.setFileName(designFile.getFileName());
                    existingFile.setFileUrl(designFile.getFileUrl());
                    existingFile.setFileSize(designFile.getFileSize());
                    existingFile.setFileType(designFile.getFileType());
                    return designFileRepository.save(existingFile);
                });
    }

    public boolean deleteFile(Long id) {
        return designFileRepository.findById(id)
                .map(file -> {
                    checkAuthorOrAdmin(file.getDesign());
                    designFileRepository.delete(file);
                    return true;
                })
                .orElse(false);
    }

    private void checkAccessPermission(Long designId) {
        if (currentUserService.isAdmin()) {
            return;
        }
        User me = currentUserService.getCurrentUser();
        Design design = designRepository.findById(designId)
                .orElseThrow(() -> new IllegalArgumentException("Design không tồn tại"));
        if (design.getDesigner() != null
                && design.getDesigner().getUser() != null
                && design.getDesigner().getUser().getId().equals(me.getId())) {
            return;
        }
        boolean hasPurchased = orderRepository.existsByUserIdAndStatusAndOrderDetailsDesignId(
                me.getId(), "PAID", designId);
        if (!hasPurchased) {
            throw new AccessDeniedException("Bạn cần mua thiết kế này trước khi xem/tải file");
        }
    }

    private void checkAuthorOrAdmin(Design design) {
        if (currentUserService.isAdmin()) {
            return;
        }
        User me = currentUserService.getCurrentUser();
        if (design.getDesigner() == null
                || design.getDesigner().getUser() == null
                || !design.getDesigner().getUser().getId().equals(me.getId())) {
            throw new AccessDeniedException("Bạn không có quyền quản lý file của thiết kế này");
        }
    }
}
