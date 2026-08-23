package com.infy.claims;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.apache.log4j.Logger;

@SpringBootApplication
public class LegacyClaimsApplication {

    private static final Logger log = Logger.getLogger(LegacyClaimsApplication.class);

    public static void main(String[] args) {
        log.info("Starting Legacy Claims Service...");
        SpringApplication.run(LegacyClaimsApplication.class, args);
        log.info("Legacy Claims Service started");
    }
}
