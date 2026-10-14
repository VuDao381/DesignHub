package com.designhub.dto;

public record FileUploadResponse(
        String originalFileName,
        String storedFileName,
        String fileUrl,
        long fileSize,
        String contentType
) {}
