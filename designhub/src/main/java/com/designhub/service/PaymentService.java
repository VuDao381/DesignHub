package com.designhub.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.entity.Cart;
import com.designhub.entity.CartItem;
import com.designhub.entity.Order;
import com.designhub.entity.Payment;
import com.designhub.repository.CartItemRepository;
import com.designhub.repository.CartRepository;
import com.designhub.repository.OrderRepository;
import com.designhub.repository.PaymentRepository;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository) {

        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
    }

    @Transactional(readOnly = true)
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId);
    }

    public Payment createPayment(Payment payment) {

        if (payment.getOrder() == null
                || payment.getOrder().getId() == null) {

            throw new IllegalArgumentException(
                    "Thanh toán phải gắn liền với một đơn hàng hợp lệ");
        }

        Order order = orderRepository
                .findById(payment.getOrder().getId())
                .orElseThrow(()
                        -> new IllegalArgumentException(
                        "Không tìm thấy đơn hàng với id: "
                        + payment.getOrder().getId()));

        if (payment.getAmount() == null
                || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Số tiền thanh toán phải lớn hơn 0");
        }

        if (payment.getAmount().compareTo(order.getTotalAmount()) != 0) {

            throw new IllegalArgumentException(
                    "Số tiền thanh toán phải bằng tổng tiền đơn hàng");
        }

        if ("PAID".equalsIgnoreCase(order.getStatus())) {

            throw new IllegalArgumentException(
                    "Đơn hàng đã được thanh toán");
        }

        payment.setOrder(order);

        if ("SUCCESS".equalsIgnoreCase(payment.getStatus())) {

            payment.setStatus("SUCCESS");

            if (payment.getPaymentDate() == null) {
                payment.setPaymentDate(LocalDateTime.now());
            }

            order.setStatus("PAID");
            orderRepository.save(order);

            clearCartAfterPayment(order);
        }

        return paymentRepository.save(payment);
    }

    public Optional<Payment> updatePaymentStatus(
            Long id,
            String status,
            String transactionId) {

        return paymentRepository.findById(id)
                .map(payment -> {

                    payment.setStatus(status);

                    if (transactionId != null
                            && !transactionId.trim().isEmpty()) {

                        payment.setTransactionId(
                                transactionId.trim());
                    }

                    if ("SUCCESS".equalsIgnoreCase(status)) {

                        if (payment.getAmount()
                                .compareTo(payment.getOrder().getTotalAmount()) != 0) {

                            throw new IllegalArgumentException(
                                    "Số tiền thanh toán phải bằng tổng tiền đơn hàng");
                        }

                        payment.setStatus("SUCCESS");
                        payment.setPaymentDate(LocalDateTime.now());

                        Order order = payment.getOrder();

                        if (order != null) {

                            if ("PAID".equalsIgnoreCase(order.getStatus())) {
                                throw new IllegalArgumentException(
                                        "Đơn hàng đã được thanh toán");
                            }

                            order.setStatus("PAID");
                            orderRepository.save(order);

                            clearCartAfterPayment(order);
                        }

                    } else if ("FAILED".equalsIgnoreCase(status)) {

                        payment.setStatus("FAILED");
                    }

                    return paymentRepository.save(payment);
                });
    }

    public Optional<Payment> updatePayment(
            Long id,
            Payment payment) {

        return paymentRepository.findById(id)
                .map(existingPayment -> {

                    if (payment.getAmount() != null) {

                        if (payment.getAmount()
                                .compareTo(BigDecimal.ZERO) <= 0) {

                            throw new IllegalArgumentException(
                                    "Số tiền thanh toán phải lớn hơn 0");
                        }

                        if (existingPayment.getOrder() != null
                                && payment.getAmount().compareTo(
                                        existingPayment.getOrder().getTotalAmount()) != 0) {

                            throw new IllegalArgumentException(
                                    "Số tiền thanh toán phải bằng tổng tiền đơn hàng");
                        }

                        existingPayment.setAmount(
                                payment.getAmount());
                    }

                    if (payment.getPaymentMethod() != null) {
                        existingPayment.setPaymentMethod(
                                payment.getPaymentMethod());
                    }

                    if (payment.getTransactionId() != null) {
                        existingPayment.setTransactionId(
                                payment.getTransactionId());
                    }

                    if (payment.getStatus() != null) {

                        if ("SUCCESS".equalsIgnoreCase(
                                payment.getStatus())) {

                            if (existingPayment.getAmount()
                                    .compareTo(existingPayment.getOrder().getTotalAmount()) != 0) {

                                throw new IllegalArgumentException(
                                        "Số tiền thanh toán phải bằng tổng tiền đơn hàng");
                            }

                            existingPayment.setStatus("SUCCESS");
                            existingPayment.setPaymentDate(
                                    LocalDateTime.now());

                            Order order
                                    = existingPayment.getOrder();

                            if (order != null) {

                                if ("PAID".equalsIgnoreCase(
                                        order.getStatus())) {

                                    throw new IllegalArgumentException(
                                            "Đơn hàng đã được thanh toán");
                                }

                                order.setStatus("PAID");
                                orderRepository.save(order);

                                clearCartAfterPayment(order);
                            }

                        } else if ("FAILED".equalsIgnoreCase(
                                payment.getStatus())) {

                            existingPayment.setStatus("FAILED");
                        } else {
                            existingPayment.setStatus(
                                    payment.getStatus());
                        }
                    }

                    return paymentRepository.save(
                            existingPayment);
                });
    }

    public boolean deletePayment(Long id) {

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

        Optional<Cart> cartOptional
                = cartRepository.findByUserId(
                        order.getUser().getId());

        if (cartOptional.isEmpty()) {
            return;
        }

        Cart cart = cartOptional.get();

        List<CartItem> cartItems
                = cartItemRepository.findByCartId(cart.getId());

        if (!cartItems.isEmpty()) {
            cartItemRepository.deleteAll(cartItems);
        }
    }
}
