package com.coubee.coubeebeproduct.domain.repository;

import com.coubee.coubeebeproduct.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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

//    List<Product> findByProductIdInOrderByProductIdDesc(List<Long> productIds);

    @Query("SELECT p FROM Product p WHERE p.productId IN :productIds")
    List<Product> findByProductIdIn(@Param("productIds") List<Long> productIds);

    Page<Product> findAllByProductNameContainingAndStoreId(String productName,Long storeId,Pageable pageable);

    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock + :quantityChange " +
            "WHERE p.productId = :productId AND p.stock + :quantityChange >= 0")
    int updateStock(@Param("productId") Long productId,
                    @Param("quantityChange") int quantityChange);
}
