package com.designhub.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.designhub.entity.OrderDetail;

public interface OrderDetailRepository
        extends JpaRepository<OrderDetail, Long> {

    List<OrderDetail> findByOrderId(Long orderId);
}
