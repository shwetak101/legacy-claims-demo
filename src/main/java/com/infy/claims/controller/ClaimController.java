package com.infy.claims.controller;

import com.infy.claims.model.Claim;
import com.infy.claims.model.FraudScore;
import com.infy.claims.service.ClaimService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/claims")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> submitClaim(@Valid @RequestBody Claim claim) {
        return ResponseEntity.ok(claimService.submitClaim(claim));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Claim> getClaim(@PathVariable String id) {
        Claim c = claimService.getClaim(id);
        return c == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(c);
    }

    @GetMapping("/{id}/fraud-score")
    public ResponseEntity<FraudScore> getFraudScore(@PathVariable String id) {
        FraudScore s = claimService.scoreFraud(id);
        return s == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(s);
    }

    @PostMapping("/{id}/reprocess")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> reprocess(@PathVariable String id) {
        claimService.reprocessClaim(id);
        return ResponseEntity.ok("reprocessed");
    }
}
