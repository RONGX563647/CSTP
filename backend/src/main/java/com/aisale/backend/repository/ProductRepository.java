package com.aisale.backend.repository;

import com.aisale.backend.entity.Product;
import com.aisale.backend.entity.Product.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findByCategory(String category, Pageable pageable);

    Page<Product> findByIsOnSaleTrueAndStatus(ProductStatus status, Pageable pageable);

    List<Product> findByIsFeaturedTrueAndStatus(ProductStatus status);

    Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
           "(:name IS NULL OR p.name LIKE %:name%) AND " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:status IS NULL OR p.status = :status)")
    Page<Product> searchProducts(
            @Param("name") String name,
            @Param("category") String category,
            @Param("minPrice") java.math.BigDecimal minPrice,
            @Param("maxPrice") java.math.BigDecimal maxPrice,
            @Param("status") ProductStatus status,
            Pageable pageable
    );

    long countByStatus(ProductStatus status);
}
