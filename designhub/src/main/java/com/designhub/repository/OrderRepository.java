package com.designhub.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.designhub.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);

    boolean existsByUserIdAndStatusAndOrderDetailsDesignId(
            Long userId,
            String status,
            Long designId);

    long countByStatus(String status);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'PAID'")
    java.math.BigDecimal calculateTotalRevenue();
}
