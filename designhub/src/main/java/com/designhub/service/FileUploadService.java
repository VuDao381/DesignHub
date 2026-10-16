package com.designhub.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.designhub.dto.FileUploadResponse;

@Service
public class FileUploadService {

    @Value("${file.upload-dir:uploads}")
    private String baseUploadDir;

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "gif", "svg");

    private static final Set<String> BLOCKED_FILE_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "sh", "jsp", "jar", "html", "htm", "js", "php", "asp", "aspx", "py", "vbs", "msi");

    public FileUploadResponse uploadImage(MultipartFile file) {
        validateFile(file);

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.png");
        String extension = getFileExtension(originalName).toLowerCase();

        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Định dạng ảnh không được hỗ trợ. Chỉ chấp nhận: "
                    + String.join(", ", ALLOWED_IMAGE_EXTENSIONS));
        }

        return storeFile(file, "images", originalName, extension);
    }

    public FileUploadResponse uploadDesignFile(MultipartFile file) {
        validateFile(file);

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file.zip");
        String extension = getFileExtension(originalName).toLowerCase();

        if (BLOCKED_FILE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Loại tệp tin nguy hiểm không được phép tải lên!");
        }

        return storeFile(file, "files", originalName, extension);
    }

    private FileUploadResponse storeFile(
            MultipartFile file,
            String subDir,
            String originalName,
            String extension) {

        try {
            Path targetDir = Paths.get(baseUploadDir, subDir).toAbsolutePath().normalize();
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            String storedFileName = UUID.randomUUID().toString()
                    + (extension.isEmpty() ? "" : "." + extension);

            Path targetPath = targetDir.resolve(storedFileName);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/uploads/" + subDir + "/" + storedFileName;

            return new FileUploadResponse(
                    originalName,
                    storedFileName,
                    fileUrl,
                    file.getSize(),
                    file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        } catch (IOException ex) {
            throw new IllegalStateException("Lỗi khi lưu trữ tệp tin: " + ex.getMessage(), ex);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Tệp tin không được để trống");
        }
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        return "";
    }
}
