package com.designhub.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.designhub.entity.Role;
import com.designhub.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsernameAndIdNot(String username, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    Optional<User> findByUsername(String username);

    boolean existsByRole(Role role);
}
