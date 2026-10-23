package com.designhub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.designhub.entity.Design;

public interface DesignRepository extends JpaRepository<Design, Long>, JpaSpecificationExecutor<Design> {

    Page<Design> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Design> findByStatus(String status, Pageable pageable);

    Page<Design> findByStatusAndNameContainingIgnoreCase(String status, String name, Pageable pageable);

    boolean existsByDesignerId(Long designerId);

    boolean existsByCategoryId(Long categoryId);

    Page<Design> findByDesignerId(Long designerId, Pageable pageable);
}
