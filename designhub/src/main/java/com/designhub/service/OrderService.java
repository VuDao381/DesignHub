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
import com.designhub.repository.CartRepository;
import com.designhub.repository.OrderDetailRepository;
import com.designhub.repository.OrderRepository;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CurrentUserService currentUserService;

    public OrderService(
            OrderRepository orderRepository,
            CartRepository cartRepository,
            OrderDetailRepository orderDetailRepository,
            CurrentUserService currentUserService) {

        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.currentUserService = currentUserService;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Order> getMyOrders() {
        User me = currentUserService.getCurrentUser();
        return orderRepository.findByUserId(me.getId());
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(Long userId) {
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
        order.setId(null);
        return orderRepository.save(order);
    }

    public Optional<Order> updateOrder(
            Long id,
            Order order) {

        return orderRepository.findById(id)
                .map(existingOrder -> {
                    existingOrder.setTotalAmount(order.getTotalAmount());
                    existingOrder.setStatus(order.getStatus());
                    existingOrder.setUser(order.getUser());

                    return orderRepository.save(existingOrder);
                });
    }

    public boolean deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            return false;
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

            orderDetailRepository.save(detail);
        }

        return savedOrder;
    }

}
