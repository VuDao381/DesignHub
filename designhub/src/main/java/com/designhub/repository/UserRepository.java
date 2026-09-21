package com.designhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.designhub.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
