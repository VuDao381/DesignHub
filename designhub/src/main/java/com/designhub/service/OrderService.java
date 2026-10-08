package com.designhub.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.designhub.entity.Cart;
import com.designhub.entity.CartItem;
import com.designhub.entity.Order;
import com.designhub.entity.OrderDetail;
import com.designhub.repository.CartRepository;
import com.designhub.repository.OrderDetailRepository;
import com.designhub.repository.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final OrderDetailRepository orderDetailRepository;

    public OrderService(
            OrderRepository orderRepository,
            CartRepository cartRepository,
            OrderDetailRepository orderDetailRepository) {

        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.orderDetailRepository = orderDetailRepository;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public Order createOrder(Order order) {
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

    public Order createOrderFromCart(Long cartId) {

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(()
                        -> new IllegalArgumentException("Cart không tồn tại"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart không có sản phẩm");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {

            BigDecimal itemTotal = item.getDesign()
                    .getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            totalAmount = totalAmount.add(itemTotal);
        }

        Order order = new Order();

        order.setUser(cart.getUser());
        order.setTotalAmount(totalAmount);
        order.setStatus("PENDING");

        Order savedOrder = orderRepository.save(order);

        for (CartItem item : cart.getItems()) {

            OrderDetail detail = new OrderDetail();

            detail.setOrder(savedOrder);
            detail.setDesign(item.getDesign());
            detail.setQuantity(item.getQuantity());
            detail.setPrice(item.getDesign().getPrice());

            orderDetailRepository.save(detail);
        }

        return savedOrder;
    }
}
