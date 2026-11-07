package com.designhub.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.designhub.entity.DesignRequest;

@Repository
public interface DesignRequestRepository extends JpaRepository<DesignRequest, Long> {

    List<DesignRequest> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<DesignRequest> findAllByOrderByCreatedAtDesc();

    @Query("SELECT r FROM DesignRequest r WHERE r.status = 'OPEN' OR r.designer.id = :designerId ORDER BY r.createdAt DESC")
    List<DesignRequest> findOpenOrAssignedToDesigner(@Param("designerId") Long designerId);
}

