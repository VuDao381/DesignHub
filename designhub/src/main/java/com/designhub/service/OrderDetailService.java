package com.designhub.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.designhub.entity.OrderDetail;
import com.designhub.repository.OrderDetailRepository;

@Service
public class OrderDetailService {

    private final OrderDetailRepository orderDetailRepository;

    public OrderDetailService(OrderDetailRepository orderDetailRepository) {
        this.orderDetailRepository = orderDetailRepository;
    }

    public List<OrderDetail> getDetailsByOrderId(Long orderId) {
        return orderDetailRepository.findByOrderId(orderId);
    }

    public Optional<OrderDetail> getDetailById(Long id) {
        return orderDetailRepository.findById(id);
    }

    public OrderDetail createDetail(OrderDetail detail) {
        return orderDetailRepository.save(detail);
    }

    public Optional<OrderDetail> updateDetail(
            Long id,
            OrderDetail detail) {

        return orderDetailRepository.findById(id)
                .map(existingDetail -> {
                    existingDetail.setQuantity(detail.getQuantity());
                    existingDetail.setPrice(detail.getPrice());
                    existingDetail.setDesign(detail.getDesign());

                    return orderDetailRepository.save(existingDetail);
                });
    }

    public boolean deleteDetail(Long id) {
        if (!orderDetailRepository.existsById(id)) {
            return false;
        }

        orderDetailRepository.deleteById(id);
        return true;
    }
}
