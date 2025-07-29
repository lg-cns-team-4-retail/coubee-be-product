package com.coubee.coubeebeproduct.domain.repository;

import com.coubee.coubeebeproduct.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product,Long> {
    Page<Product> findAllByStoreId(Long storeId, Pageable pageable);
}
