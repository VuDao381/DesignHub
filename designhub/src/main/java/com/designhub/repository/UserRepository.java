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

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByRole(Role role);

    long countByRole(Role role);

    @org.springframework.data.jpa.repository.Query("""
        SELECT new com.designhub.dto.UserGrowthDTO(
            YEAR(u.createdAt),
            MONTH(u.createdAt),
            COUNT(u.id)
        )
        FROM User u
        WHERE u.createdAt IS NOT NULL
        GROUP BY YEAR(u.createdAt), MONTH(u.createdAt)
        ORDER BY YEAR(u.createdAt) ASC, MONTH(u.createdAt) ASC
    """)
    java.util.List<com.designhub.dto.UserGrowthDTO> getMonthlyUserGrowth();
}
