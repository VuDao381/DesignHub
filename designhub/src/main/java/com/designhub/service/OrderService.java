package com.designhub.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.entity.User;
import com.designhub.security.CurrentUserService;
import com.designhub.entity.Cart;
import com.designhub.entity.CartItem;
import com.designhub.entity.Order;
import com.designhub.entity.OrderDetail;
import com.designhub.entity.Payment;
import com.designhub.entity.PaymentStatus;
import com.designhub.exception.ResourceNotFoundException;
import com.designhub.repository.CartItemRepository;
import com.designhub.repository.CartRepository;
import com.designhub.repository.OrderDetailRepository;
import com.designhub.repository.OrderRepository;
import com.designhub.repository.PaymentRepository;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final PaymentRepository paymentRepository;
    private final CurrentUserService currentUserService;

    public OrderService(
            OrderRepository orderRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            OrderDetailRepository orderDetailRepository,
            PaymentRepository paymentRepository,
            CurrentUserService currentUserService) {

        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.paymentRepository = paymentRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        if (!currentUserService.isAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Quản trị viên (ADMIN) mới có quyền xem tất cả đơn hàng");
        }
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Order> getMyOrders() {
        User me = currentUserService.getCurrentUser();
        return orderRepository.findByUserId(me.getId());
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(Long userId) {
        currentUserService.checkOwnerOrAdmin(userId);
        return orderRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(order -> {
                    currentUserService.checkOwnerOrAdmin(
                            order.getUser().getId());
                    return order;
                });
    }

    public Order createOrder(Order order) {
        if (!currentUserService.isAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Quản trị viên (ADMIN) mới có quyền tạo đơn hàng thủ công");
        }
        order.setId(null);
        return orderRepository.save(order);
    }

    public Optional<Order> updateOrder(
            Long id,
            Order order) {
        if (!currentUserService.isAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Quản trị viên (ADMIN) mới có quyền cập nhật đơn hàng");
        }

        return orderRepository.findById(id)
                .map(existingOrder -> {
                    if (order.getTotalAmount() != null) {
                        existingOrder.setTotalAmount(order.getTotalAmount());
                    }
                    if (order.getStatus() != null && !order.getStatus().isBlank()) {
                        existingOrder.setStatus(order.getStatus().trim().toUpperCase());
                    }
                    if (order.getUser() != null && order.getUser().getId() != null) {
                        existingOrder.setUser(order.getUser());
                    }

                    return orderRepository.save(existingOrder);
                });
    }

    public boolean deleteOrder(Long id) {
        if (!currentUserService.isAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Quản trị viên (ADMIN) mới có quyền xóa đơn hàng");
        }
        if (!orderRepository.existsById(id)) {
            return false;
        }

        List<Payment> payments = paymentRepository.findByOrderId(id);
        if (!payments.isEmpty()) {
            paymentRepository.deleteAll(payments);
        }

        orderRepository.deleteById(id);
        return true;
    }

    public Order checkout() {
        User me = currentUserService.getCurrentUser();
        Cart cart = cartRepository.findByUserId(me.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Bạn chưa có giỏ hàng"));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng trống");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            var design = item.getDesign();
            if (design == null) {
                throw new IllegalArgumentException("Sản phẩm trong giỏ hàng không hợp lệ");
            }

            if (!"APPROVED".equalsIgnoreCase(design.getStatus())) {
                throw new IllegalArgumentException("Thiết kế '" + design.getName() + "' chưa được phê duyệt để bán");
            }

            if (design.getDesigner() != null
                    && design.getDesigner().getUser() != null
                    && design.getDesigner().getUser().getId().equals(me.getId())) {
                throw new IllegalArgumentException("Bạn không thể mua thiết kế của chính mình: " + design.getName());
            }

            if (orderRepository.existsByUserIdAndStatusAndOrderDetailsDesignId(me.getId(), "PAID", design.getId())) {
                throw new IllegalArgumentException("Bạn đã mua thiết kế này rồi: " + design.getName());
            }

            BigDecimal itemTotal = design.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
        }

        List<Order> oldPendingOrders = orderRepository.findByUserId(me.getId())
                .stream()
                .filter(o -> "PENDING".equalsIgnoreCase(o.getStatus()))
                .toList();
        for (Order oldOrder : oldPendingOrders) {
            oldOrder.setStatus("CANCELLED");
            orderRepository.save(oldOrder);

            List<Payment> oldPayments = paymentRepository.findByOrderId(oldOrder.getId());
            for (Payment p : oldPayments) {
                if (PaymentStatus.PENDING.equals(p.getStatus())) {
                    p.setStatus(PaymentStatus.CANCELLED);
                    paymentRepository.save(p);
                }
            }
        }

        Order order = new Order();
        order.setId(null);
        order.setUser(me);
        order.setTotalAmount(totalAmount);
        order.setStatus("PENDING");

        Order savedOrder = orderRepository.save(order);

        for (CartItem item : cart.getItems()) {
            OrderDetail detail = new OrderDetail();
            detail.setId(null);
            detail.setOrder(savedOrder);
            detail.setDesign(item.getDesign());
            detail.setQuantity(item.getQuantity());
            detail.setPrice(item.getDesign().getPrice());

            OrderDetail savedDetail = orderDetailRepository.save(detail);
            savedOrder.getOrderDetails().add(savedDetail);
        }

        return savedOrder;
    }

    public Order approveOrder(Long orderId) {
        if (!currentUserService.isAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Quản trị viên (ADMIN) mới có quyền duyệt đơn hàng");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));

        if (!"PENDING".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể duyệt đơn hàng đang ở trạng thái PENDING. Trạng thái hiện tại: " + order.getStatus());
        }

        order.setStatus("PAID");
        Order savedOrder = orderRepository.save(order);

        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        boolean hasSuccessPayment = false;
        for (Payment p : payments) {
            if (PaymentStatus.SUCCESS.equals(p.getStatus())) {
                hasSuccessPayment = true;
            } else if (PaymentStatus.PENDING.equals(p.getStatus())) {
                if (!hasSuccessPayment) {
                    p.setStatus(PaymentStatus.SUCCESS);
                    p.setPaymentDate(java.time.LocalDateTime.now());
                    if (p.getTransactionId() == null || p.getTransactionId().isBlank()) {
                        p.setTransactionId("ADMIN_APPR_" + System.currentTimeMillis());
                    }
                    paymentRepository.save(p);
                    hasSuccessPayment = true;
                } else {
                    p.setStatus(PaymentStatus.CANCELLED);
                    paymentRepository.save(p);
                }
            }
        }

        if (!hasSuccessPayment) {
            Payment payment = new Payment();
            payment.setOrder(savedOrder);
            payment.setAmount(savedOrder.getTotalAmount());
            payment.setPaymentMethod("ADMIN_DIRECT_APPROVAL");
            payment.setTransactionId("ADMIN_APPR_" + System.currentTimeMillis());
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaymentDate(java.time.LocalDateTime.now());
            paymentRepository.save(payment);
        }

        if (order.getUser() != null && order.getUser().getId() != null) {
            java.util.Set<Long> orderedDesignIds = orderDetailRepository.findByOrderId(orderId).stream()
                    .map(od -> od.getDesign().getId())
                    .collect(java.util.stream.Collectors.toSet());
            cartRepository.findByUserId(order.getUser().getId()).ifPresent(cart -> {
                List<CartItem> toRemove = cartItemRepository.findByCartId(cart.getId()).stream()
                        .filter(ci -> ci.getDesign() != null && orderedDesignIds.contains(ci.getDesign().getId()))
                        .toList();
                if (!toRemove.isEmpty()) {
                    if (cart.getItems() != null) {
                        cart.getItems().removeAll(toRemove);
                    }
                    cartItemRepository.deleteAll(toRemove);
                }
            });
        }

        return savedOrder;
    }

}
