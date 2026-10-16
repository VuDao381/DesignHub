package com.designhub.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardResponse(
        BigDecimal totalRevenue,
        long totalOrders,
        long totalUsers,
        long totalDesigners,
        long totalDesigns,
        List<UserGrowthDTO> userGrowth,
        List<TopSellingDesignDTO> topSellingDesigns
) {}
