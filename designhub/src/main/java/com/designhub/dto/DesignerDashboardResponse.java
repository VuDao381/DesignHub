package com.designhub.dto;

import java.math.BigDecimal;
import java.util.List;

public record DesignerDashboardResponse(
        Long designerId,
        String designerName,
        BigDecimal totalRevenue,
        long totalSold,
        long totalDownloads,
        long totalFavorites,
        List<TopSellingDesignDTO> topDesigns
) {}
