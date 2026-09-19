package com.park.ecommerce.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsByProductCode(String productCode);

    List<Product> findAllByProductCodeIn(Collection<String> productCodes);

    Page<Product> findAllByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findAllByStatusAndCategoryId(ProductStatus status, Long categoryId, Pageable pageable);

    Optional<Product> findByIdAndStatus(Long id, ProductStatus status);
}
