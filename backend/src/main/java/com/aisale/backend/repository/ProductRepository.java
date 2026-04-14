package com.aisale.backend.repository;

import com.aisale.backend.entity.Product;
import com.aisale.backend.entity.Product.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findByCategory(String category, Pageable pageable);

    Page<Product> findByIsOnSaleTrueAndStatus(ProductStatus status, Pageable pageable);

    List<Product> findByIsFeaturedTrueAndStatus(ProductStatus status);

    Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    Page<Product> findBySellerId(Long sellerId, Pageable pageable);

    List<Product> findBySellerId(Long sellerId);

    @Query("SELECT p FROM Product p WHERE " +
           "(:name IS NULL OR p.name LIKE %:name%) AND " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:sellerId IS NULL OR p.sellerId = :sellerId)")
    Page<Product> searchProducts(
            @Param("name") String name,
            @Param("category") String category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("status") ProductStatus status,
            @Param("sellerId") Long sellerId,
            Pageable pageable
    );

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN p.tags t WHERE " +
           "p.status = :status AND " +
           "(:keyword IS NULL OR " +
           "  LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "  LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "  LOWER(p.category) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "  LOWER(t) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> searchMultiField(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("status") ProductStatus status,
            Pageable pageable
    );

    @Query("SELECT p FROM Product p WHERE p.status = :status ORDER BY p.salesCount DESC")
    List<Product> findTopBySalesCount(@Param("status") ProductStatus status, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.status = :status ORDER BY p.viewCount DESC")
    List<Product> findTopByViewCount(@Param("status") ProductStatus status, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.status = :status AND p.category = :category ORDER BY p.salesCount DESC")
    List<Product> findTopByCategoryAndSalesCount(@Param("status") ProductStatus status, @Param("category") String category, Pageable pageable);

    long countByStatus(ProductStatus status);

    long countBySellerId(Long sellerId);
}