package com.designhub.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
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
@Transactional
public class DesignFileService {

    @Value("${file.upload-dir:uploads}")
    private String baseUploadDir;

    private final DesignFileRepository designFileRepository;
    private final DesignRepository designRepository;
    private final OrderRepository orderRepository;
    private final CurrentUserService currentUserService;
    private final DownloadHistoryService downloadHistoryService;

    public DesignFileService(
            DesignFileRepository designFileRepository,
            DesignRepository designRepository,
            OrderRepository orderRepository,
            CurrentUserService currentUserService,
            DownloadHistoryService downloadHistoryService) {
        this.designFileRepository = designFileRepository;
        this.designRepository = designRepository;
        this.orderRepository = orderRepository;
        this.currentUserService = currentUserService;
        this.downloadHistoryService = downloadHistoryService;
    }

    public record DownloadableFile(
            Resource resource,
            String fileName,
            String contentType,
            long contentLength
    ) {}

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

        designFile.setId(null);
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
                    try {
                        String storedFileName = Paths.get(file.getFileUrl()).getFileName().toString();
                        Path baseDir = Paths.get(baseUploadDir, "files").toAbsolutePath().normalize();
                        Path filePath = baseDir.resolve(storedFileName).normalize();
                        if (filePath.startsWith(baseDir)) {
                            Files.deleteIfExists(filePath);
                        }
                    } catch (Exception ignored) {
                    }
                    return true;
                })
                .orElse(false);
    }

    public DownloadableFile getFileForDownload(Long fileId) {
        DesignFile file = designFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tệp thiết kế với ID: " + fileId));

        // 1. Kiểm tra quyền truy cập: Admin, Tác giả thiết kế hoặc Khách hàng đã thanh toán (Order status = PAID)
        checkAccessPermission(file.getDesign().getId());

        // 2. Định vị file an toàn trên hệ thống lưu trữ
        String storedFileName = Paths.get(file.getFileUrl()).getFileName().toString();
        Path baseDir = Paths.get(baseUploadDir, "files").toAbsolutePath().normalize();
        Path filePath = baseDir.resolve(storedFileName).normalize();

        // Chống lỗ hổng Path Traversal
        if (!filePath.startsWith(baseDir)) {
            throw new SecurityException("Đường dẫn tệp tin không hợp lệ");
        }

        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new IllegalStateException("Tệp tin không tồn tại hoặc không thể đọc trên máy chủ lưu trữ");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            long contentLength = Files.size(filePath);
            String contentType = file.getFileType();
            if (contentType == null || contentType.isBlank()) {
                contentType = Files.probeContentType(filePath);
            }
            if (contentType == null || contentType.isBlank()) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            // 3. Tự động ghi nhận lịch sử tải xuống
            try {
                downloadHistoryService.createDownloadHistory(file.getDesign().getId());
            } catch (Exception ex) {
                // Không làm gián đoạn tải nếu có lỗi phụ khi ghi log
            }

            return new DownloadableFile(resource, file.getFileName(), contentType, contentLength);
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("URL tệp tin không hợp lệ: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Không thể đọc tệp tin: " + ex.getMessage(), ex);
        }
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
