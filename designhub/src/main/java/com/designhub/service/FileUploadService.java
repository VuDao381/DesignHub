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
            "jpg", "jpeg", "png", "webp", "gif");

    private static final Set<String> BLOCKED_FILE_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "sh", "jsp", "jar", "html", "htm", "js", "php", "asp", "aspx",
            "py", "vbs", "msi", "ps1", "psm1", "dll", "com", "scr", "phtml", "phar", "war",
            "ear", "cgi", "pl", "rb", "wsf", "hta", "xhtml");

    public FileUploadResponse uploadImage(MultipartFile file) {
        validateFile(file);

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Kích thước hình ảnh không được vượt quá 10MB");
        }

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.png");
        String extension = getFileExtension(originalName).toLowerCase();

        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Định dạng ảnh không được hỗ trợ. Chỉ chấp nhận: "
                    + String.join(", ", ALLOWED_IMAGE_EXTENSIONS));
        }

        validateImageMagicBytes(file, extension);

        return storeFile(file, "images", originalName, extension);
    }

    private void validateImageMagicBytes(MultipartFile file, String extension) {
        try (var is = file.getInputStream()) {
            byte[] header = new byte[12];
            int bytesRead = is.read(header);
            if (bytesRead < 4) {
                throw new IllegalArgumentException("Tệp tin quá nhỏ hoặc không hợp lệ");
            }

            boolean valid = false;
            switch (extension) {
                case "jpg":
                case "jpeg":
                    // FF D8 FF
                    valid = (header[0] & 0xFF) == 0xFF
                            && (header[1] & 0xFF) == 0xD8
                            && (header[2] & 0xFF) == 0xFF;
                    break;
                case "png":
                    // 89 50 4E 47 0D 0A 1A 0A
                    valid = bytesRead >= 8
                            && (header[0] & 0xFF) == 0x89
                            && (header[1] & 0xFF) == 0x50
                            && (header[2] & 0xFF) == 0x4E
                            && (header[3] & 0xFF) == 0x47
                            && (header[4] & 0xFF) == 0x0D
                            && (header[5] & 0xFF) == 0x0A
                            && (header[6] & 0xFF) == 0x1A
                            && (header[7] & 0xFF) == 0x0A;
                    break;
                case "gif":
                    // GIF8
                    valid = (header[0] & 0xFF) == 0x47
                            && (header[1] & 0xFF) == 0x49
                            && (header[2] & 0xFF) == 0x46
                            && (header[3] & 0xFF) == 0x38;
                    break;
                case "webp":
                    // RIFF .... WEBP
                    valid = bytesRead >= 12
                            && (header[0] & 0xFF) == 0x52
                            && (header[1] & 0xFF) == 0x49
                            && (header[2] & 0xFF) == 0x46
                            && (header[3] & 0xFF) == 0x46
                            && (header[8] & 0xFF) == 0x57
                            && (header[9] & 0xFF) == 0x45
                            && (header[10] & 0xFF) == 0x42
                            && (header[11] & 0xFF) == 0x50;
                    break;
                default:
                    valid = false;
            }

            if (!valid) {
                throw new IllegalArgumentException(
                        "Nội dung tệp không khớp với định dạng hình ảnh hợp lệ (magic bytes không hợp lệ)");
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Không thể đọc tệp để xác thực định dạng ảnh", ex);
        }
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
