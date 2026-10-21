package com.designhub.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.designhub.entity.DesignFile;
import com.designhub.service.DesignFileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/design-files")
@Tag(name = "Design Files", description = "API quản lý và tải xuống tệp tin thiết kế")
public class DesignFileController {

    private final DesignFileService designFileService;

    public DesignFileController(DesignFileService designFileService) {
        this.designFileService = designFileService;
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Tải xuống tệp thiết kế an toàn",
               description = "Kiểm tra quyền sở hữu/đơn hàng đã thanh toán, ghi nhận lịch sử tải và stream file nhị phân.")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        DesignFileService.DownloadableFile downloadableFile = designFileService.getFileForDownload(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(downloadableFile.contentType()))
                .contentLength(downloadableFile.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(downloadableFile.fileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(downloadableFile.resource());
    }

    @GetMapping
    public ResponseEntity<List<DesignFile>> getAllFiles() {
        return ResponseEntity.ok(
                designFileService.getAllFiles()
        );
    }

    @GetMapping("/design/{designId}")
    public ResponseEntity<List<DesignFile>> getFilesByDesignId(
            @PathVariable Long designId) {

        return ResponseEntity.ok(
                designFileService.getFilesByDesignId(designId)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<DesignFile> getFileById(
            @PathVariable Long id) {

        return designFileService.getFileById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<DesignFile> createFile(
            @Valid @RequestBody DesignFile designFile) {

        return ResponseEntity.ok(
                designFileService.createFile(designFile)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<DesignFile> updateFile(
            @PathVariable Long id,
            @Valid @RequestBody DesignFile designFile) {

        return designFileService.updateFile(id, designFile)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable Long id) {

        if (!designFileService.deleteFile(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}
