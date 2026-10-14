package com.designhub.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.entity.Order;
import com.designhub.entity.OrderDetail;
import com.designhub.repository.OrderDetailRepository;
import com.designhub.repository.OrderRepository;
import com.designhub.security.CurrentUserService;

@Service
@Transactional
public class OrderDetailService {

    private final OrderDetailRepository orderDetailRepository;
    private final OrderRepository orderRepository;
    private final CurrentUserService currentUserService;

    public OrderDetailService(
            OrderDetailRepository orderDetailRepository,
            OrderRepository orderRepository,
            CurrentUserService currentUserService) {
        this.orderDetailRepository = orderDetailRepository;
        this.orderRepository = orderRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<OrderDetail> getDetailsByOrderId(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
        currentUserService.checkOwnerOrAdmin(order.getUser().getId());
        return orderDetailRepository.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public Optional<OrderDetail> getDetailById(Long id) {
        return orderDetailRepository.findById(id)
                .map(detail -> {
                    currentUserService.checkOwnerOrAdmin(detail.getOrder().getUser().getId());
                    return detail;
                });
    }

    public OrderDetail createDetail(OrderDetail detail) {
        if (!currentUserService.isAdmin()) {
            throw new AccessDeniedException("Chỉ ADMIN mới được tạo chi tiết đơn hàng thủ công");
        }
        if (detail.getOrder() == null || detail.getOrder().getId() == null) {
            throw new IllegalArgumentException("OrderDetail phải gắn với Order hợp lệ");
        }
        Order order = orderRepository.findById(detail.getOrder().getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        detail.setId(null);
        detail.setOrder(order);
        return orderDetailRepository.save(detail);
    }

    public Optional<OrderDetail> updateDetail(
            Long id,
            OrderDetail detail) {

        if (!currentUserService.isAdmin()) {
            throw new AccessDeniedException("Chỉ ADMIN mới được chỉnh sửa chi tiết đơn hàng");
        }

        return orderDetailRepository.findById(id)
                .map(existingDetail -> {
                    existingDetail.setQuantity(detail.getQuantity());
                    existingDetail.setPrice(detail.getPrice());
                    if (detail.getDesign() != null) {
                        existingDetail.setDesign(detail.getDesign());
                    }

                    return orderDetailRepository.save(existingDetail);
                });
    }

    public boolean deleteDetail(Long id) {
        if (!currentUserService.isAdmin()) {
            throw new AccessDeniedException("Chỉ ADMIN mới được xoá chi tiết đơn hàng");
        }

        if (!orderDetailRepository.existsById(id)) {
            return false;
        }

        orderDetailRepository.deleteById(id);
        return true;
    }
}
