package com.park.ecommerce.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsByProductCode(String productCode);

    List<Product> findAllByProductCodeIn(Collection<String> productCodes);
}
