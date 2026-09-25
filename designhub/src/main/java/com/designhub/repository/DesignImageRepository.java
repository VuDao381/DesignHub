package com.designhub.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.designhub.entity.DesignImage;

public interface DesignImageRepository extends JpaRepository<DesignImage, Long> {

    List<DesignImage> findByDesignId(Long designId);
}
