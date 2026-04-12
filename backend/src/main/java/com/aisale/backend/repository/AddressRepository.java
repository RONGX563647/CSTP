package com.aisale.backend.repository;

import com.aisale.backend.entity.Address;
import com.aisale.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserAndStatusOrderByIsDefaultDescCreatedAtDesc(User user, Address.AddressStatus status);

    Optional<Address> findByUserAndIdAndStatus(User user, Long id, Address.AddressStatus status);

    Optional<Address> findByUserAndIsDefaultTrueAndStatus(User user, Address.AddressStatus status);

    @Modifying
    @Query("UPDATE Address a SET a.isDefault = ?3 WHERE a.user = ?1 AND a.status = ?2")
    void updateIsDefaultByUserAndStatus(User user, Address.AddressStatus status, Boolean isDefault);

    long countByUserAndStatus(User user, Address.AddressStatus status);
}