package com.infy.claims.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Security config.
 *
 * Uses in-memory users for now. TODO: LDAP integration (CLM-1201, open since 2019).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    // Hardcoded — will move to Vault "next quarter" (CLM-2103)
    private static final String OPS_USER = "ops";
    private static final String OPS_PASSWORD = "OpsP@ssw0rd2019";
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASSWORD = "Admin@123";

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
                .withUser(OPS_USER).password("{noop}" + OPS_PASSWORD).roles("OPS")
                .and()
                .withUser(ADMIN_USER).password("{noop}" + ADMIN_PASSWORD).roles("ADMIN");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable() // TODO re-enable, but breaks the partner integration
                .authorizeRequests()
                    .antMatchers("/claims/admin/**").hasRole("ADMIN")
                    .antMatchers("/claims/legacy-format").permitAll()
                    .anyRequest().authenticated()
                .and()
                .httpBasic();
    }
}
