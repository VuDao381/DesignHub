package com.designhub.dto;

import java.math.BigDecimal;

public record TopSellingDesignDTO(
        Long designId,
        String designName,
        BigDecimal price,
        String designerName,
        Long totalSold,
        BigDecimal totalRevenue,
        String imageUrl
) {
    public TopSellingDesignDTO(
            Long designId,
            String designName,
            BigDecimal price,
            String designerName,
            Long totalSold,
            BigDecimal totalRevenue) {
        this(designId, designName, price, designerName, totalSold, totalRevenue, null);
    }

    public TopSellingDesignDTO(
            Long designId,
            String designName,
            BigDecimal price,
            String designerName,
            Long totalSold,
            Object totalRevenueObj) {
        this(
                designId,
                designName,
                price,
                designerName,
                totalSold != null ? totalSold : 0L,
                totalRevenueObj instanceof BigDecimal bd ? bd
                        : (totalRevenueObj instanceof Number num ? BigDecimal.valueOf(num.doubleValue()) : BigDecimal.ZERO),
                null
        );
    }

    public TopSellingDesignDTO(
            Long designId,
            String designName,
            BigDecimal price,
            String designerName,
            Long totalSold,
            Object totalRevenueObj,
            String imageUrl) {
        this(
                designId,
                designName,
                price,
                designerName,
                totalSold != null ? totalSold : 0L,
                totalRevenueObj instanceof BigDecimal bd ? bd
                        : (totalRevenueObj instanceof Number num ? BigDecimal.valueOf(num.doubleValue()) : BigDecimal.ZERO),
                imageUrl
        );
    }
}
