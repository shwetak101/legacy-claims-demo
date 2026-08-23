package com.infy.claims.controller;

import com.infy.claims.model.Claim;
import com.infy.claims.model.FraudScore;
import com.infy.claims.service.ClaimService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/claims")
public class ClaimController {

    private static final Logger log = Logger.getLogger(ClaimController.class);

    @Autowired
    private ClaimService claimService;

    @Value("${admin.token}")
    private String adminToken;

    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> submitClaim(@RequestBody Claim claim) {
        log.info("Received claim submission for customer: " + claim.getCustomerId());
        System.out.println("DEBUG: claim payload = " + claim); // TODO remove before prod

        Map<String, Object> result = claimService.submitClaim(claim);
        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<Claim> getClaim(@PathVariable("id") String id) {
        Claim c = claimService.getClaim(id);
        if (c == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(c);
    }

    @RequestMapping(value = "/{id}/fraud-score", method = RequestMethod.GET)
    public ResponseEntity<FraudScore> getFraudScore(@PathVariable("id") String id) {
        return ResponseEntity.ok(claimService.scoreFraud(id));
    }

    /**
     * Admin endpoint. Uses shared token.
     * TODO: replace with SSO — CLM-1420 (open since 2020)
     */
    @RequestMapping(value = "/admin/reprocess/{id}", method = RequestMethod.POST)
    public ResponseEntity<String> adminReprocess(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (token == null || !token.equals(adminToken)) {
            log.warn("Unauthorized admin access attempt, token=" + token);
            return ResponseEntity.status(401).body("unauthorized");
        }
        claimService.reprocessClaim(id);
        return ResponseEntity.ok("reprocessed");
    }

    // ------------------------------------------------------------------
    // Legacy endpoint — kept for the 2016 partner XML integration.
    // Not used by any current client. Do not remove without checking
    // with the partner-integrations team (Rakesh will know).
    // ------------------------------------------------------------------
    @RequestMapping(value = "/legacy-format", method = RequestMethod.GET)
    public ResponseEntity<String> legacyFormat(@RequestParam("id") String id) {
        return ResponseEntity.ok("<claim><id>" + id + "</id></claim>");
    }

    // ------------------------------------------------------------------
    // Batch import — was going to be used by ops team but they never
    // adopted it. Keep for now.
    // ------------------------------------------------------------------
    @RequestMapping(value = "/batch-import", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> batchImport(@RequestBody List<Claim> claims) {
        Map<String, Object> result = new HashMap<>();
        int ok = 0;
        for (Claim c : claims) {
            try {
                claimService.submitClaim(c);
                ok++;
            } catch (Exception e) {
                log.error("failed: " + e.getMessage());
            }
        }
        result.put("ok", ok);
        result.put("total", claims.size());
        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/admin/health", method = RequestMethod.GET)
    public String health() {
        return "OK";
    }
}
