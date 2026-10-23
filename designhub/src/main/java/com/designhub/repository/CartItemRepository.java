package com.designhub.repository;

import com.designhub.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartId(Long cartId);

    Optional<CartItem> findByCartIdAndDesignId(Long cartId, Long designId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.design.id = :designId")
    void deleteByDesignId(@Param("designId") Long designId);
}
