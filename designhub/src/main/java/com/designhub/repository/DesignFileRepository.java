package com.designhub.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.designhub.entity.DesignFile;

public interface DesignFileRepository extends JpaRepository<DesignFile, Long> {

    List<DesignFile> findByDesignId(Long designId);
}
