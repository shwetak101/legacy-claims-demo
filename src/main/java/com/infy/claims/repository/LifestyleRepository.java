package com.infy.claims.repository;

import com.infy.claims.model.Lifestyle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LifestyleRepository extends JpaRepository<Lifestyle, String> {
    Optional<Lifestyle> findByCustomerId(String customerId);
}
