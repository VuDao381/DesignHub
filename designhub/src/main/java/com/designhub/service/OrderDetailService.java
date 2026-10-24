package com.designhub.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.entity.Design;
import com.designhub.entity.Order;
import com.designhub.entity.OrderDetail;
import com.designhub.exception.ResourceNotFoundException;
import com.designhub.repository.DesignRepository;
import com.designhub.repository.OrderDetailRepository;
import com.designhub.repository.OrderRepository;
import com.designhub.security.CurrentUserService;

@Service
@Transactional
public class OrderDetailService {

    private final OrderDetailRepository orderDetailRepository;
    private final OrderRepository orderRepository;
    private final DesignRepository designRepository;
    private final CurrentUserService currentUserService;

    public OrderDetailService(
            OrderDetailRepository orderDetailRepository,
            OrderRepository orderRepository,
            CurrentUserService currentUserService) {
        this(orderDetailRepository, orderRepository, null, currentUserService);
    }

    @Autowired
    public OrderDetailService(
            OrderDetailRepository orderDetailRepository,
            OrderRepository orderRepository,
            DesignRepository designRepository,
            CurrentUserService currentUserService) {
        this.orderDetailRepository = orderDetailRepository;
        this.orderRepository = orderRepository;
        this.designRepository = designRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<OrderDetail> getDetailsByOrderId(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        if (detail.getDesign() == null || detail.getDesign().getId() == null) {
            throw new IllegalArgumentException("OrderDetail phải gắn với Design hợp lệ");
        }
        if (designRepository != null) {
            Design design = designRepository.findById(detail.getDesign().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thiết kế"));
            detail.setDesign(design);
            if (detail.getPrice() == null) {
                detail.setPrice(design.getPrice());
            }
        }

        detail.setId(null);
        detail.setOrder(order);
        OrderDetail saved = orderDetailRepository.save(detail);
        recalculateOrderTotal(order);
        return saved;
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
                    if (detail.getDesign() != null && detail.getDesign().getId() != null) {
                        if (designRepository != null) {
                            Design design = designRepository.findById(detail.getDesign().getId())
                                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thiết kế"));
                            existingDetail.setDesign(design);
                        } else {
                            existingDetail.setDesign(detail.getDesign());
                        }
                    }

                    OrderDetail saved = orderDetailRepository.save(existingDetail);
                    recalculateOrderTotal(existingDetail.getOrder());
                    return saved;
                });
    }

    public boolean deleteDetail(Long id) {
        if (!currentUserService.isAdmin()) {
            throw new AccessDeniedException("Chỉ ADMIN mới được xoá chi tiết đơn hàng");
        }

        Optional<OrderDetail> detailOpt = orderDetailRepository.findById(id);
        if (detailOpt.isEmpty()) {
            return false;
        }

        Order order = detailOpt.get().getOrder();
        if (order != null && order.getOrderDetails() != null) {
            order.getOrderDetails().remove(detailOpt.get());
        }
        orderDetailRepository.delete(detailOpt.get());
        orderDetailRepository.flush();
        if (order != null) {
            recalculateOrderTotal(order);
        }
        return true;
    }

    private void recalculateOrderTotal(Order order) {
        if (order == null || order.getId() == null) {
            return;
        }
        List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
        BigDecimal total = BigDecimal.ZERO;
        for (OrderDetail d : details) {
            if (d.getPrice() != null) {
                total = total.add(d.getPrice().multiply(BigDecimal.valueOf(d.getQuantity())));
            }
        }
        order.setTotalAmount(total);
        orderRepository.save(order);
    }
}
