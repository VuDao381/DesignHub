package com.designhub.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.designhub.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);
}
