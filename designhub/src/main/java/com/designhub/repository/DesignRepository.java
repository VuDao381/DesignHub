package com.designhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.designhub.entity.Design;

public interface DesignRepository extends JpaRepository<Design, Long> {
}
