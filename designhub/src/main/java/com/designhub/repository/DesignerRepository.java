package com.designhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.designhub.entity.Designer;

public interface DesignerRepository extends JpaRepository<Designer, Long> {
}
