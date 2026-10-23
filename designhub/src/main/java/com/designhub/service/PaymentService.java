package com.designhub.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.entity.Cart;
import com.designhub.entity.CartItem;
import com.designhub.entity.Order;
import com.designhub.entity.Payment;
import com.designhub.entity.PaymentStatus;
import com.designhub.exception.ResourceNotFoundException;
import com.designhub.repository.CartItemRepository;
import com.designhub.repository.CartRepository;
import com.designhub.repository.OrderRepository;
import com.designhub.repository.PaymentRepository;
import com.designhub.security.CurrentUserService;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CurrentUserService currentUserService;

    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            CurrentUserService currentUserService) {

        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<Payment> getAllPayments() {
        if (!currentUserService.isAdmin()) {
            throw new AccessDeniedException("Chỉ Quản trị viên (ADMIN) mới có quyền xem tất cả thanh toán");
        }
        return paymentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .map(payment -> {
                    if (payment.getOrder() != null && payment.getOrder().getUser() != null) {
                        currentUserService.checkOwnerOrAdmin(payment.getOrder().getUser().getId());
                    }
                    return payment;
                });
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByOrderId(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        currentUserService.checkOwnerOrAdmin(order.getUser().getId());
        return paymentRepository.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
                .map(payment -> {
                    if (payment.getOrder() != null && payment.getOrder().getUser() != null) {
                        currentUserService.checkOwnerOrAdmin(payment.getOrder().getUser().getId());
                    }
                    return payment;
                });
    }

    public Payment createPayment(Payment payment) {
        payment.setId(null);

        if (payment.getOrder() == null || payment.getOrder().getId() == null) {
            throw new IllegalArgumentException(
                    "Thanh toán phải gắn liền với một đơn hàng hợp lệ");
        }

        Order order = orderRepository
                .findById(payment.getOrder().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn hàng với id: "
                                + payment.getOrder().getId()));

        currentUserService.checkOwnerOrAdmin(order.getUser().getId());

        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Số tiền thanh toán phải lớn hơn 0");
        }

        if (payment.getAmount().compareTo(order.getTotalAmount()) != 0) {
            throw new IllegalArgumentException(
                    "Số tiền thanh toán phải bằng tổng tiền đơn hàng");
        }

        if (!"PENDING".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalArgumentException(
                    "Chỉ có thể tạo thanh toán cho đơn hàng đang chờ xử lý (PENDING). Trạng thái hiện tại: " + order.getStatus());
        }

        payment.setOrder(order);

        if (!currentUserService.isAdmin()) {
            payment.setStatus(PaymentStatus.PENDING);
            payment.setPaymentDate(null);
        } else if (PaymentStatus.SUCCESS.equals(payment.getStatus())) {
            applyPaymentSuccess(payment, order);
        }

        return paymentRepository.save(payment);
    }

    public Optional<Payment> updatePaymentStatus(
            Long id,
            PaymentStatus status,
            String transactionId) {

        if (!currentUserService.isAdmin()) {
            throw new AccessDeniedException(
                    "Chỉ Quản trị viên (ADMIN) mới có quyền cập nhật trạng thái thanh toán");
        }

        if (status == null) {
            throw new IllegalArgumentException("Trạng thái thanh toán không được để trống");
        }

        return paymentRepository.findById(id)
                .map(payment -> {
                    payment.setStatus(status);

                    if (transactionId != null && !transactionId.trim().isEmpty()) {
                        payment.setTransactionId(transactionId.trim());
                    }

                    if (PaymentStatus.SUCCESS.equals(status)) {
                        applyPaymentSuccess(payment, payment.getOrder());
                    }

                    return paymentRepository.save(payment);
                });
    }

    public Payment confirmPayment(Long id, String transactionId) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin thanh toán với ID: " + id));

        if (payment.getOrder() == null || payment.getOrder().getUser() == null) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng liên kết với thanh toán này");
        }

        currentUserService.checkOwnerOrAdmin(payment.getOrder().getUser().getId());

        if (!PaymentStatus.PENDING.equals(payment.getStatus())) {
            throw new IllegalArgumentException(
                    "Chỉ có thể xác nhận giao dịch đang ở trạng thái chờ (PENDING). Trạng thái hiện tại: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());
        if (transactionId != null && !transactionId.trim().isEmpty()) {
            payment.setTransactionId(transactionId.trim());
        } else if (payment.getTransactionId() == null || payment.getTransactionId().isBlank()) {
            payment.setTransactionId("TXN_CONF_" + System.currentTimeMillis());
        }

        applyPaymentSuccess(payment, payment.getOrder());
        return paymentRepository.save(payment);
    }

    public Optional<Payment> updatePayment(
            Long id,
            Payment payment) {

        if (!currentUserService.isAdmin()) {
            throw new AccessDeniedException("Chỉ Quản trị viên (ADMIN) mới có quyền chỉnh sửa thanh toán");
        }

        return paymentRepository.findById(id)
                .map(existingPayment -> {
                    if (payment.getAmount() != null) {
                        if (payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                            throw new IllegalArgumentException(
                                     "Số tiền thanh toán phải lớn hơn 0");
                        }

                        if (existingPayment.getOrder() != null
                                && payment.getAmount().compareTo(
                                        existingPayment.getOrder().getTotalAmount()) != 0) {
                            throw new IllegalArgumentException(
                                    "Số tiền thanh toán phải bằng tổng tiền đơn hàng");
                        }

                        existingPayment.setAmount(payment.getAmount());
                    }

                    if (payment.getPaymentMethod() != null) {
                        existingPayment.setPaymentMethod(payment.getPaymentMethod());
                    }

                    if (payment.getTransactionId() != null) {
                        existingPayment.setTransactionId(payment.getTransactionId());
                    }

                    if (payment.getStatus() != null) {
                        if (PaymentStatus.SUCCESS.equals(payment.getStatus())) {
                            applyPaymentSuccess(existingPayment, existingPayment.getOrder());
                        } else {
                            existingPayment.setStatus(payment.getStatus());
                        }
                    }

                    return paymentRepository.save(existingPayment);
                });
    }

    private void applyPaymentSuccess(Payment payment, Order order) {
        if (order == null) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng liên kết để thanh toán");
        }

        if (payment.getAmount() == null
                || payment.getAmount().compareTo(order.getTotalAmount()) != 0) {
            throw new IllegalArgumentException(
                    "Số tiền thanh toán phải bằng tổng tiền đơn hàng");
        }

        if (!"PENDING".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể xác nhận thanh toán thành công cho đơn hàng đang ở trạng thái PENDING. Trạng thái hiện tại: " + order.getStatus());
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDateTime.now());
        }

        order.setStatus("PAID");
        orderRepository.save(order);

        if (order.getId() != null) {
            List<Payment> siblingPayments = paymentRepository.findByOrderId(order.getId());
            for (Payment other : siblingPayments) {
                if (other != payment
                        && (payment.getId() == null || !payment.getId().equals(other.getId()))
                        && PaymentStatus.PENDING.equals(other.getStatus())) {
                    other.setStatus(PaymentStatus.CANCELLED);
                    paymentRepository.save(other);
                }
            }
        }

        clearCartAfterPayment(order);
    }

    public boolean deletePayment(Long id) {
        if (!currentUserService.isAdmin()) {
            throw new AccessDeniedException("Chỉ Quản trị viên (ADMIN) mới có quyền xóa thanh toán");
        }

        if (!paymentRepository.existsById(id)) {
            return false;
        }

        paymentRepository.deleteById(id);
        return true;
    }

    private void clearCartAfterPayment(Order order) {

        if (order.getUser() == null
                || order.getUser().getId() == null) {
            return;
        }

        Optional<Cart> cartOptional = cartRepository.findByUserId(
                order.getUser().getId());

        if (cartOptional.isEmpty()) {
            return;
        }

        Cart cart = cartOptional.get();

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());

        if (!cartItems.isEmpty()) {
            java.util.Set<Long> orderedDesignIds = order.getOrderDetails() != null
                    ? order.getOrderDetails().stream()
                            .filter(od -> od.getDesign() != null && od.getDesign().getId() != null)
                            .map(od -> od.getDesign().getId())
                            .collect(java.util.stream.Collectors.toSet())
                    : java.util.Set.of();

            List<CartItem> toRemove = orderedDesignIds.isEmpty()
                    ? cartItems
                    : cartItems.stream()
                            .filter(ci -> ci.getDesign() != null && orderedDesignIds.contains(ci.getDesign().getId()))
                            .toList();

            if (!toRemove.isEmpty()) {
                if (cart.getItems() != null) {
                    cart.getItems().removeAll(toRemove);
                }
                cartItemRepository.deleteAll(toRemove);
            }
        }
    }
}
