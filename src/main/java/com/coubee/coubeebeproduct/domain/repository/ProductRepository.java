package com.coubee.coubeebeproduct.domain.repository;

import com.coubee.coubeebeproduct.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("""
                SELECT p FROM Product p
                WHERE p.storeId = :storeId
                AND (
                    LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                    LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
                AND p.status = 'ACTIVE'
            """)
    Page<Product> searchByKeyword(
            @Param("storeId") Long storeId,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
