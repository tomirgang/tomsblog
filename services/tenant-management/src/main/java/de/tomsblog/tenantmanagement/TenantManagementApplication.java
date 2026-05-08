package de.tomsblog.tenantmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Tenant Management Service application entry point (ADR-0031).
 *
 * @req SWR-072
 */
@SpringBootApplication
public class TenantManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenantManagementApplication.class, args);
    }
}
