package com.designhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.designhub.entity.DesignImage;

public interface DesignImageRepository extends JpaRepository<DesignImage, Long> {
}
