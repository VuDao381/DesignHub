package com.designhub.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.designhub.entity.DownloadHistory;
import com.designhub.service.DownloadHistoryService;

@RestController
@RequestMapping("/api/download-history")
public class DownloadHistoryController {

    private final DownloadHistoryService downloadHistoryService;

    public DownloadHistoryController(
            DownloadHistoryService downloadHistoryService) {
        this.downloadHistoryService = downloadHistoryService;
    }

    @GetMapping
    public ResponseEntity<List<DownloadHistory>> getAllDownloadHistories() {
        return ResponseEntity.ok(
                downloadHistoryService.getAllDownloadHistories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DownloadHistory> getDownloadHistoryById(
            @PathVariable Long id) {

        return downloadHistoryService
                .getDownloadHistoryById(id)
                .map(ResponseEntity::ok)
                .orElseGet(()
                        -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DownloadHistory>> getDownloadHistoriesByUserId(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                downloadHistoryService
                        .getDownloadHistoriesByUserId(userId));
    }

    @GetMapping("/design/{designId}")
    public ResponseEntity<List<DownloadHistory>> getDownloadHistoriesByDesignId(
            @PathVariable Long designId) {

        return ResponseEntity.ok(
                downloadHistoryService
                        .getDownloadHistoriesByDesignId(designId));
    }

    @GetMapping("/user/{userId}/design/{designId}")
    public ResponseEntity<DownloadHistory>
            getDownloadHistoryByUserAndDesign(
                    @PathVariable Long userId,
                    @PathVariable Long designId) {

        return downloadHistoryService
                .getDownloadHistoryByUserAndDesign(userId, designId)
                .map(ResponseEntity::ok)
                .orElseGet(()
                        -> ResponseEntity.notFound().build());
    }

    @PostMapping("/user/{userId}/design/{designId}")
    public ResponseEntity<DownloadHistory> createDownloadHistory(
            @PathVariable Long userId,
            @PathVariable Long designId) {

        DownloadHistory history
                = downloadHistoryService
                        .createDownloadHistory(userId, designId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(history);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDownloadHistory(
            @PathVariable Long id) {

        if (!downloadHistoryService.deleteDownloadHistory(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}
