package com.designhub.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.designhub.dto.FileUploadResponse;
import com.designhub.service.FileUploadService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/upload")
@Tag(name = "File Upload", description = "API tải lên hình ảnh và tệp tin thiết kế")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tải lên hình ảnh", description = "Hỗ trợ các định dạng: jpg, jpeg, png, webp, gif. Tối đa 10MB.")
    public ResponseEntity<FileUploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(fileUploadService.uploadImage(file));
    }

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tải lên tệp thiết kế gốc", description = "Dành cho Designer/Admin tải file thiết kế (zip, rar, psd, ai, fig, v.v.). Tối đa 100MB.")
    public ResponseEntity<FileUploadResponse> uploadDesignFile(
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(fileUploadService.uploadDesignFile(file));
    }
}
