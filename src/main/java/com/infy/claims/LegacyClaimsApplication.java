package com.infy.claims;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class LegacyClaimsApplication {

    private static final Logger log = LoggerFactory.getLogger(LegacyClaimsApplication.class);

    public static void main(String[] args) {
        log.info("Starting Legacy Claims Service...");
        SpringApplication.run(LegacyClaimsApplication.class, args);
        log.info("Legacy Claims Service started");
    }
}
