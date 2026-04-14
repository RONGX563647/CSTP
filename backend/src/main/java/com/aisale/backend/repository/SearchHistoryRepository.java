package com.aisale.backend.repository;

import com.aisale.backend.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    Optional<SearchHistory> findByUserIdAndKeyword(Long userId, String keyword);

    @Query("SELECT s FROM SearchHistory s WHERE s.userId = :userId ORDER BY s.lastSearchTime DESC")
    List<SearchHistory> findByUserIdOrderByLastSearchTimeDesc(Long userId);

    @Query("SELECT s FROM SearchHistory s ORDER BY s.searchCount DESC")
    List<SearchHistory> findTopKeywords();

    @Query("SELECT s.keyword FROM SearchHistory s GROUP BY s.keyword ORDER BY SUM(s.searchCount) DESC")
    List<String> findHotKeywords();

    void deleteByUserId(Long userId);
}