package com.coubee.coubeebeproduct.domain.repository;

import com.coubee.coubeebeproduct.domain.Product;
import com.coubee.coubeebeproduct.domain.ProductViewRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductViewRecordRepository extends JpaRepository<ProductViewRecord, Long> {

    @Query("SELECT pvr FROM ProductViewRecord pvr JOIN FETCH pvr.product WHERE pvr.product.productId = :productId AND pvr.userId = :userId")
    Optional<ProductViewRecord> findByProductIdAndUserId(@Param("productId") Long productId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM ProductViewRecord p WHERE p.product = :product")
    void deleteByProduct(@Param("product") Product product);
}
