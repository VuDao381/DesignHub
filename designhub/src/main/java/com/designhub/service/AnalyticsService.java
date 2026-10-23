package com.designhub.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final UserRepository userRepository;
    private final DesignerRepository designerRepository;
    private final DesignRepository designRepository;
    private final DownloadHistoryRepository downloadHistoryRepository;
    private final FavoriteRepository favoriteRepository;
    private final CurrentUserService currentUserService;

    public AnalyticsService(
            OrderRepository orderRepository,
            OrderDetailRepository orderDetailRepository,
            UserRepository userRepository,
            DesignerRepository designerRepository,
            DesignRepository designRepository,
            DownloadHistoryRepository downloadHistoryRepository,
            FavoriteRepository favoriteRepository,
            CurrentUserService currentUserService) {

        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.userRepository = userRepository;
        this.designerRepository = designerRepository;
        this.designRepository = designRepository;
        this.downloadHistoryRepository = downloadHistoryRepository;
        this.favoriteRepository = favoriteRepository;
        this.currentUserService = currentUserService;
    }

    public AdminDashboardResponse getAdminDashboard() {
        if (!currentUserService.isAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Quản trị viên (ADMIN) mới có quyền xem thống kê tổng quan");
        }
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue();
        long totalOrders = orderRepository.countByStatus("PAID");
        long totalUsers = userRepository.count();
        long totalDesigners = designerRepository.count();
        long totalDesigns = designRepository.count();

        List<UserGrowthDTO> userGrowth = userRepository.getMonthlyUserGrowth();
        List<TopSellingDesignDTO> topSellingDesigns = orderDetailRepository.findTopSellingDesigns(PageRequest.of(0, 10));

        return new AdminDashboardResponse(
                totalRevenue != null ? totalRevenue : BigDecimal.ZERO,
                totalOrders,
                totalUsers,
                totalDesigners,
                totalDesigns,
                userGrowth,
                topSellingDesigns
        );
    }

    public DesignerDashboardResponse getDesignerDashboard() {
        User me = currentUserService.getCurrentUser();
        java.util.Optional<Designer> designerOpt = designerRepository.findByUserId(me.getId());
        if (designerOpt.isEmpty()) {
            if (currentUserService.isAdmin() || me.getRole() == com.designhub.entity.Role.DESIGNER) {
                return new DesignerDashboardResponse(
                        null,
                        me.getFullName(),
                        BigDecimal.ZERO,
                        0L,
                        0L,
                        0L,
                        List.of()
                );
            }
            throw new IllegalArgumentException("Tài khoản chưa có hồ sơ Designer");
        }
        Designer designer = designerOpt.get();

        Long designerId = designer.getId();

        BigDecimal totalRevenue = orderDetailRepository.calculateDesignerRevenue(designerId);
        long totalSold = orderDetailRepository.countDesignerSales(designerId);
        long totalDownloads = downloadHistoryRepository.countByDesignerId(designerId);
        long totalFavorites = favoriteRepository.countByDesignerId(designerId);
        List<TopSellingDesignDTO> topDesigns = orderDetailRepository.findTopDesignsByDesigner(designerId, PageRequest.of(0, 10));

        return new DesignerDashboardResponse(
                designerId,
                designer.getUser().getFullName(),
                totalRevenue != null ? totalRevenue : BigDecimal.ZERO,
                totalSold,
                totalDownloads,
                totalFavorites,
                topDesigns
        );
    }
}
