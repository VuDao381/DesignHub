package com.designhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.designhub.entity.Design;
import com.designhub.entity.Designer;
import com.designhub.entity.User;
import com.designhub.repository.CartItemRepository;
import com.designhub.repository.DesignRepository;
import com.designhub.repository.DesignerRepository;
import com.designhub.repository.OrderDetailRepository;
import com.designhub.security.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class DesignServiceTest {

    @Mock
    private DesignRepository designRepository;

    @Mock
    private DesignerRepository designerRepository;

    @Mock
    private OrderDetailRepository orderDetailRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CurrentUserService currentUserService;

    private DesignService designService;

    private User sampleUser;
    private Designer sampleDesigner;
    private Design sampleDesign;

    @BeforeEach
    void setUp() {
        designService = new DesignService(
                designRepository,
                designerRepository,
                orderDetailRepository,
                cartItemRepository,
                currentUserService);

        sampleUser = new User();
        sampleUser.setId(10L);
        sampleUser.setUsername("designer_user");

        sampleDesigner = new Designer();
        sampleDesigner.setId(20L);
        sampleDesigner.setUser(sampleUser);

        sampleDesign = new Design();
        sampleDesign.setId(100L);
        sampleDesign.setName("Thiết kế logo");
        sampleDesign.setPrice(new BigDecimal("200000.00"));
        sampleDesign.setStatus("APPROVED");
        sampleDesign.setDesigner(sampleDesigner);
    }

    @Test
    void testGetMyDesignsSuccess() {
        when(currentUserService.getCurrentUser()).thenReturn(sampleUser);
        when(designerRepository.findByUserId(10L)).thenReturn(Optional.of(sampleDesigner));
        Pageable pageable = PageRequest.of(0, 10);
        Page<Design> page = new PageImpl<>(List.of(sampleDesign));
        when(designRepository.findByDesignerId(20L, pageable)).thenReturn(page);

        Page<Design> result = designService.getMyDesigns(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Thiết kế logo", result.getContent().get(0).getName());
    }

    @Test
    void testUpdateDesignStatusRejectedRequiresReason() {
        when(currentUserService.isAdmin()).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> designService.updateDesignStatus(100L, "REJECTED", "   "));
        assertTrue(ex.getMessage().contains("rejectReason"));
    }

    @Test
    void testUpdateDesignStatusRejectSuccess() {
        when(currentUserService.isAdmin()).thenReturn(true);
        when(designRepository.findById(100L)).thenReturn(Optional.of(sampleDesign));
        when(designRepository.save(any(Design.class))).thenAnswer(i -> i.getArgument(0));

        Optional<Design> updated = designService.updateDesignStatus(100L, "REJECTED", "Ảnh vi phạm bản quyền");

        assertTrue(updated.isPresent());
        assertEquals("REJECTED", updated.get().getStatus());
        assertEquals("Ảnh vi phạm bản quyền", updated.get().getRejectReason());
        verify(cartItemRepository).deleteByDesignId(100L);
    }

    @Test
    void testUpdateDesignStatusApprovedClearsReason() {
        sampleDesign.setStatus("REJECTED");
        sampleDesign.setRejectReason("Lỗi cũ");

        when(currentUserService.isAdmin()).thenReturn(true);
        when(designRepository.findById(100L)).thenReturn(Optional.of(sampleDesign));
        when(designRepository.save(any(Design.class))).thenAnswer(i -> i.getArgument(0));

        Optional<Design> updated = designService.updateDesignStatus(100L, "APPROVED", null);

        assertTrue(updated.isPresent());
        assertEquals("APPROVED", updated.get().getStatus());
        assertNull(updated.get().getRejectReason());
    }

    @Test
    void testDeleteDesignSoftDeletesWhenHasOrders() {
        when(designRepository.findById(100L)).thenReturn(Optional.of(sampleDesign));
        when(currentUserService.isAdmin()).thenReturn(true);
        when(orderDetailRepository.existsByDesignId(100L)).thenReturn(true);

        boolean result = designService.deleteDesign(100L);

        assertTrue(result);
        assertEquals("ARCHIVED", sampleDesign.getStatus());
        verify(designRepository).save(sampleDesign);
        verify(designRepository, never()).delete(sampleDesign);
        verify(cartItemRepository).deleteByDesignId(100L);
    }

    @Test
    void testDeleteDesignCleansCartAndHardDeletesWhenNoOrders() {
        when(designRepository.findById(100L)).thenReturn(Optional.of(sampleDesign));
        when(currentUserService.isAdmin()).thenReturn(true);
        when(orderDetailRepository.existsByDesignId(100L)).thenReturn(false);

        boolean result = designService.deleteDesign(100L);

        assertTrue(result);
        verify(cartItemRepository).deleteByDesignId(100L);
        verify(designRepository).delete(sampleDesign);
    }
}
