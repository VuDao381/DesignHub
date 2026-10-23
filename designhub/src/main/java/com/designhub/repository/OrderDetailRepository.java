package com.designhub.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.designhub.dto.TopSellingDesignDTO;
import com.designhub.entity.OrderDetail;

public interface OrderDetailRepository
        extends JpaRepository<OrderDetail, Long> {

    List<OrderDetail> findByOrderId(Long orderId);

    boolean existsByDesignId(Long designId);

    @Query("""
        SELECT new com.designhub.dto.TopSellingDesignDTO(
            od.design.id,
            od.design.name,
            od.design.price,
            od.design.designer.user.fullName,
            COALESCE(SUM(od.quantity), 0),
            COALESCE(SUM(od.price * od.quantity), 0)
        )
        FROM OrderDetail od
        WHERE od.order.status = 'PAID'
        GROUP BY od.design.id, od.design.name, od.design.price, od.design.designer.user.fullName
        ORDER BY COALESCE(SUM(od.quantity), 0) DESC
    """)
    List<TopSellingDesignDTO> findTopSellingDesigns(Pageable pageable);

    @Query("""
        SELECT COALESCE(SUM(od.price * od.quantity), 0)
        FROM OrderDetail od
        WHERE od.order.status = 'PAID'
          AND od.design.designer.id = :designerId
    """)
    BigDecimal calculateDesignerRevenue(@Param("designerId") Long designerId);

    @Query("""
        SELECT COALESCE(SUM(od.quantity), 0)
        FROM OrderDetail od
        WHERE od.order.status = 'PAID'
          AND od.design.designer.id = :designerId
    """)
    long countDesignerSales(@Param("designerId") Long designerId);

    @Query("""
        SELECT new com.designhub.dto.TopSellingDesignDTO(
            od.design.id,
            od.design.name,
            od.design.price,
            od.design.designer.user.fullName,
            COALESCE(SUM(od.quantity), 0),
            COALESCE(SUM(od.price * od.quantity), 0)
        )
        FROM OrderDetail od
        WHERE od.order.status = 'PAID'
          AND od.design.designer.id = :designerId
        GROUP BY od.design.id, od.design.name, od.design.price, od.design.designer.user.fullName
        ORDER BY COALESCE(SUM(od.quantity), 0) DESC
    """)
    List<TopSellingDesignDTO> findTopDesignsByDesigner(@Param("designerId") Long designerId, Pageable pageable);
}
