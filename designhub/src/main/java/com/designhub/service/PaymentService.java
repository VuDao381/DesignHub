package com.designhub.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.entity.Order;
import com.designhub.entity.Payment;
import com.designhub.repository.OrderRepository;
import com.designhub.repository.PaymentRepository;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
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
        if (payment.getOrder() == null || payment.getOrder().getId() == null) {
            throw new IllegalArgumentException("Thanh toán phải gắn liền với một đơn hàng hợp lệ");
        }

        Order order = orderRepository.findById(payment.getOrder().getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng với id: " + payment.getOrder().getId()));

        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0");
        }

        payment.setOrder(order);

        if ("SUCCESS".equalsIgnoreCase(payment.getStatus())) {
            payment.setStatus("SUCCESS");
            if (payment.getPaymentDate() == null) {
                payment.setPaymentDate(LocalDateTime.now());
            }
            order.setStatus("PAID");
            orderRepository.save(order);
        }

        return paymentRepository.save(payment);
    }

    public Optional<Payment> updatePaymentStatus(Long id, String status, String transactionId) {
        return paymentRepository.findById(id)
                .map(payment -> {
                    payment.setStatus(status);
                    if (transactionId != null && !transactionId.trim().isEmpty()) {
                        payment.setTransactionId(transactionId.trim());
                    }

                    if ("SUCCESS".equalsIgnoreCase(status)) {
                        payment.setStatus("SUCCESS");
                        payment.setPaymentDate(LocalDateTime.now());

                        Order order = payment.getOrder();
                        if (order != null) {
                            order.setStatus("PAID");
                            orderRepository.save(order);
                        }
                    } else if ("FAILED".equalsIgnoreCase(status)) {
                        payment.setStatus("FAILED");
                    }

                    return paymentRepository.save(payment);
                });
    }

    public Optional<Payment> updatePayment(Long id, Payment payment) {
        return paymentRepository.findById(id)
                .map(existingPayment -> {
                    if (payment.getAmount() != null) {
                        if (payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                            throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0");
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
                        existingPayment.setStatus(payment.getStatus());
                        if ("SUCCESS".equalsIgnoreCase(payment.getStatus())) {
                            existingPayment.setPaymentDate(LocalDateTime.now());
                            Order order = existingPayment.getOrder();
                            if (order != null) {
                                order.setStatus("PAID");
                                orderRepository.save(order);
                            }
                        }
                    }

                    return paymentRepository.save(existingPayment);
                });
    }

    public boolean deletePayment(Long id) {
        if (!paymentRepository.existsById(id)) {
            return false;
        }

        paymentRepository.deleteById(id);
        return true;
    }
}
