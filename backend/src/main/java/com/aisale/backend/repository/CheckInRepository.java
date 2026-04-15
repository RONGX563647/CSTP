package com.aisale.backend.repository;

import com.aisale.backend.entity.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CheckInRepository extends JpaRepository<CheckIn, Long> {

    Optional<CheckIn> findByUserIdAndCheckInDate(Long userId, LocalDate checkInDate);

    List<CheckIn> findByUserIdAndCheckInDateBetweenOrderByCheckInDateAsc(Long userId, LocalDate startDate, LocalDate endDate);

    Optional<CheckIn> findTopByUserIdOrderByCheckInDateDesc(Long userId);

    long countByUserId(Long userId);
}
