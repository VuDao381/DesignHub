package com.designhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.designhub.dto.AdminDashboardResponse;
import com.designhub.dto.DesignerDashboardResponse;
import com.designhub.dto.TopSellingDesignDTO;
import com.designhub.dto.UserGrowthDTO;
import com.designhub.entity.Designer;
import com.designhub.entity.User;
import com.designhub.repository.DesignRepository;
import com.designhub.repository.DesignerRepository;
import com.designhub.repository.DownloadHistoryRepository;
import com.designhub.repository.FavoriteRepository;
import com.designhub.repository.OrderDetailRepository;
import com.designhub.repository.OrderRepository;
import com.designhub.repository.UserRepository;
import com.designhub.security.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderDetailRepository orderDetailRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DesignerRepository designerRepository;

    @Mock
    private DesignRepository designRepository;

    @Mock
    private DownloadHistoryRepository downloadHistoryRepository;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private CurrentUserService currentUserService;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(
                orderRepository,
                orderDetailRepository,
                userRepository,
                designerRepository,
                designRepository,
                downloadHistoryRepository,
                favoriteRepository,
                currentUserService
        );
    }

    @Test
    void testGetAdminDashboard() {
        when(currentUserService.isAdmin()).thenReturn(true);
        when(orderRepository.calculateTotalRevenue()).thenReturn(BigDecimal.valueOf(5000000));
        when(orderRepository.countByStatus("PAID")).thenReturn(25L);
        when(userRepository.count()).thenReturn(100L);
        when(designerRepository.count()).thenReturn(10L);
        when(designRepository.count()).thenReturn(50L);

        when(userRepository.getMonthlyUserGrowth()).thenReturn(List.of(new UserGrowthDTO(2026, 10, 5L)));
        when(orderDetailRepository.findTopSellingDesigns(any(Pageable.class)))
                .thenReturn(List.of(new TopSellingDesignDTO(1L, "Logo Brand", BigDecimal.valueOf(200000), "Designer A", 10L, BigDecimal.valueOf(2000000))));

        AdminDashboardResponse response = analyticsService.getAdminDashboard();

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(5000000), response.totalRevenue());
        assertEquals(25L, response.totalOrders());
        assertEquals(100L, response.totalUsers());
        assertEquals(10L, response.totalDesigners());
        assertEquals(50L, response.totalDesigns());
        assertEquals(1, response.userGrowth().size());
        assertEquals(1, response.topSellingDesigns().size());
    }

    @Test
    void testGetDesignerDashboard() {
        User me = new User();
        me.setId(5L);
        me.setFullName("Designer Tran");

        Designer designer = new Designer();
        designer.setId(2L);
        designer.setUser(me);

        when(currentUserService.getCurrentUser()).thenReturn(me);
        when(designerRepository.findByUserId(5L)).thenReturn(Optional.of(designer));

        when(orderDetailRepository.calculateDesignerRevenue(2L)).thenReturn(BigDecimal.valueOf(1500000));
        when(orderDetailRepository.countDesignerSales(2L)).thenReturn(8L);
        when(downloadHistoryRepository.countByDesignerId(2L)).thenReturn(40L);
        when(favoriteRepository.countByDesignerId(2L)).thenReturn(95L);

        when(orderDetailRepository.findTopDesignsByDesigner(eq(2L), any(Pageable.class)))
                .thenReturn(List.of(new TopSellingDesignDTO(10L, "Banner Web", BigDecimal.valueOf(150000), "Designer Tran", 5L, BigDecimal.valueOf(750000))));

        DesignerDashboardResponse response = analyticsService.getDesignerDashboard();

        assertNotNull(response);
        assertEquals(2L, response.designerId());
        assertEquals("Designer Tran", response.designerName());
        assertEquals(BigDecimal.valueOf(1500000), response.totalRevenue());
        assertEquals(8L, response.totalSold());
        assertEquals(40L, response.totalDownloads());
        assertEquals(95L, response.totalFavorites());
        assertEquals(1, response.topDesigns().size());
    }
}
