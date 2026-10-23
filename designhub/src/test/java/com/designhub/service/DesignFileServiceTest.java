package com.designhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.designhub.entity.Design;
import com.designhub.entity.DesignFile;
import com.designhub.entity.Designer;
import com.designhub.entity.User;
import com.designhub.repository.DesignFileRepository;
import com.designhub.repository.DesignRepository;
import com.designhub.repository.OrderRepository;
import com.designhub.security.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class DesignFileServiceTest {

    @Mock
    private DesignFileRepository designFileRepository;

    @Mock
    private DesignRepository designRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private DownloadHistoryService downloadHistoryService;

    private DesignFileService designFileService;

    private User sampleUser;
    private Designer sampleDesigner;
    private Design sampleDesign;
    private DesignFile sampleFile;

    @BeforeEach
    void setUp() {
        designFileService = new DesignFileService(
                designFileRepository,
                designRepository,
                orderRepository,
                currentUserService,
                downloadHistoryService);

        sampleUser = new User();
        sampleUser.setId(10L);

        sampleDesigner = new Designer();
        sampleDesigner.setId(20L);
        sampleDesigner.setUser(sampleUser);

        sampleDesign = new Design();
        sampleDesign.setId(100L);
        sampleDesign.setStatus("APPROVED");
        sampleDesign.setDesigner(sampleDesigner);

        sampleFile = new DesignFile();
        sampleFile.setId(50L);
        sampleFile.setDesign(sampleDesign);
        sampleFile.setFileName("source.psd");
        sampleFile.setFileUrl("/uploads/files/test.psd");
        sampleFile.setFileSize(1024L);
    }

    @Test
    void testCreateFileResetsDesignToPendingWhenApproved() {
        when(designRepository.findById(100L)).thenReturn(Optional.of(sampleDesign));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.getCurrentUser()).thenReturn(sampleUser);
        when(designFileRepository.save(any(DesignFile.class))).thenAnswer(i -> i.getArgument(0));

        DesignFile newFile = new DesignFile();
        newFile.setDesign(sampleDesign);
        newFile.setFileName("new_vector.ai");

        DesignFile saved = designFileService.createFile(newFile);

        assertNotNull(saved);
        assertEquals("PENDING", sampleDesign.getStatus());
        verify(designRepository).save(sampleDesign);
    }

    @Test
    void testUpdateFileResetsDesignToPendingWhenApproved() {
        when(designFileRepository.findById(50L)).thenReturn(Optional.of(sampleFile));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.getCurrentUser()).thenReturn(sampleUser);
        when(designFileRepository.save(any(DesignFile.class))).thenAnswer(i -> i.getArgument(0));

        DesignFile updateData = new DesignFile();
        updateData.setFileName("updated.psd");
        updateData.setFileUrl("/uploads/files/updated.psd");
        updateData.setFileSize(2048L);

        Optional<DesignFile> result = designFileService.updateFile(50L, updateData);

        assertTrue(result.isPresent());
        assertEquals("PENDING", sampleDesign.getStatus());
        verify(designRepository).save(sampleDesign);
    }

    @Test
    void testDeleteFileResetsDesignToPendingWhenApproved() {
        when(designFileRepository.findById(50L)).thenReturn(Optional.of(sampleFile));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.getCurrentUser()).thenReturn(sampleUser);

        boolean result = designFileService.deleteFile(50L);

        assertTrue(result);
        assertEquals("PENDING", sampleDesign.getStatus());
        verify(designRepository).save(sampleDesign);
        verify(designFileRepository).delete(sampleFile);
    }
}
