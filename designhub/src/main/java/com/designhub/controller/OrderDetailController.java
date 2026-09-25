package com.designhub.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.designhub.entity.OrderDetail;
import com.designhub.service.OrderDetailService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/order-details")
public class OrderDetailController {

    private final OrderDetailService orderDetailService;

    public OrderDetailController(OrderDetailService orderDetailService) {
        this.orderDetailService = orderDetailService;
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<OrderDetail>> getDetailsByOrderId(
            @PathVariable Long orderId) {

        return ResponseEntity.ok(
                orderDetailService.getDetailsByOrderId(orderId)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetail> getDetailById(
            @PathVariable Long id) {

        return orderDetailService.getDetailById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<OrderDetail> createDetail(
            @Valid @RequestBody OrderDetail detail) {

        return ResponseEntity.ok(
                orderDetailService.createDetail(detail)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderDetail> updateDetail(
            @PathVariable Long id,
            @Valid @RequestBody OrderDetail detail) {

        return orderDetailService.updateDetail(id, detail)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDetail(
            @PathVariable Long id) {

        if (!orderDetailService.deleteDetail(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}
