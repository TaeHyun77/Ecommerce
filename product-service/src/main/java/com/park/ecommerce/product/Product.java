package com.park.ecommerce.product;

import com.park.ecommerce.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Product extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String productCode; // 상품코드 (SKU)

    @Column(nullable = false)
    private String name;

    private String brand;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StorageType storageType; // 보관 방법 - 상온, 냉장, 냉동

    @Column(nullable = false)
    private Integer price; // 판매가 (원)

    private String thumbnailUrl;

    @Column(nullable = false)
    private Long categoryId; // 연관관계 없이 식별자만 저장 - DB FK 제약은 없음

    @Builder
    private Product(
            String productCode, String name, String brand, String description,
            StorageType storageType, Integer price,
            String thumbnailUrl, Long categoryId
    ) {
        validatePrice(price);
        validateCategoryId(categoryId);

        this.productCode = productCode;
        this.name = name;
        this.brand = brand;
        this.description = description;
        this.status = ProductStatus.ON_SALE; // 상태는 판매중으로 고정
        this.storageType = storageType;
        this.price = price;
        this.thumbnailUrl = thumbnailUrl;
        this.categoryId = categoryId;
    }

    private static void validatePrice(Integer price) {
        if (price == null || price < 0) {
            throw new IllegalArgumentException("판매가는 0원 이상이어야 합니다.");
        }
    }

    private static void validateCategoryId(Long categoryId) {
        if (categoryId == null) {
            throw new IllegalArgumentException("카테고리는 필수입니다.");
        }
    }
}
