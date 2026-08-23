package com.infy.claims.repository;

import com.infy.claims.model.Claim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClaimRepository extends JpaRepository<Claim, String> {
    List<Claim> findByCustomerIdAndStatus(String customerId, String status);
    long countByCustomerIdAndStatus(String customerId, String status);
}
