package com.designhub.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.designhub.dto.FileUploadResponse;

class FileUploadServiceTest {

    private FileUploadService fileUploadService;
    private Path tempUploadDir;

    @BeforeEach
    void setUp() throws IOException {
        fileUploadService = new FileUploadService();
        tempUploadDir = Files.createTempDirectory("test-uploads");
        ReflectionTestUtils.setField(fileUploadService, "baseUploadDir", tempUploadDir.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(tempUploadDir)) {
            try (var stream = Files.walk(tempUploadDir)) {
                stream.sorted((a, b) -> b.compareTo(a))
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ignored) {
                            }
                        });
            }
        }
    }

    @Test
    void testUploadImageSuccess() {
        byte[] validPngBytes = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D
        };
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", validPngBytes);

        FileUploadResponse response = fileUploadService.uploadImage(file);

        assertNotNull(response);
        assertEquals("avatar.png", response.originalFileName());
        assertTrue(response.fileUrl().startsWith("/uploads/images/"));
        assertTrue(response.storedFileName().endsWith(".png"));
    }

    @Test
    void testUploadImageFailsInvalidMagicBytes() {
        MockMultipartFile fakePng = new MockMultipartFile(
                "file", "malicious.png", "image/png", "NOT_A_REAL_PNG".getBytes());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> fileUploadService.uploadImage(fakePng));
        assertTrue(ex.getMessage().contains("magic bytes không hợp lệ"));
    }

    @Test
    void testUploadImageRejectsExecutable() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/octet-stream", "bad-content".getBytes());

        assertThrows(IllegalArgumentException.class, () -> fileUploadService.uploadImage(file));
    }

    @Test
    void testUploadDesignFileSuccess() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "poster-pack.zip", "application/zip", "zip-content".getBytes());

        FileUploadResponse response = fileUploadService.uploadDesignFile(file);

        assertNotNull(response);
        assertTrue(response.fileUrl().startsWith("/uploads/files/"));
    }

    @Test
    void testUploadDesignFileBlocksScript() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "exploit.sh", "text/x-shellscript", "echo hack".getBytes());

        assertThrows(IllegalArgumentException.class, () -> fileUploadService.uploadDesignFile(file));
    }

    private static void assertEquals(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
