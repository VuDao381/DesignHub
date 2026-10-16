package com.designhub.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.designhub.dto.AdminDashboardResponse;
import com.designhub.dto.DesignerDashboardResponse;
import com.designhub.service.AnalyticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics & Dashboard", description = "API thống kê doanh thu và báo cáo phân tích")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/admin")
    @Operation(summary = "Thống kê tổng quan cho Admin",
               description = "Dành riêng cho ADMIN: Tổng doanh thu sàn, tăng trưởng người dùng, top thiết kế bán chạy.")
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard() {
        return ResponseEntity.ok(analyticsService.getAdminDashboard());
    }

    @GetMapping("/designer")
    @Operation(summary = "Thống kê thu nhập cá nhân cho Designer",
               description = "Dành cho DESIGNER: Doanh thu cá nhân từ các mẫu đã bán, lượt tải, lượt yêu thích, top mẫu của tác giả.")
    public ResponseEntity<DesignerDashboardResponse> getDesignerDashboard() {
        return ResponseEntity.ok(analyticsService.getDesignerDashboard());
    }
}
