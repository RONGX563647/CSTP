package com.aisale.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AiSaleBackendApplication {

    public static void main(String[] args) {
        // Load .env file before Spring Boot starts
        try {
            Dotenv dotenv = Dotenv.load();
            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                if (System.getenv(key) == null) {
                    System.setProperty(key, value);
                }
            });
        } catch (Exception e) {
            // .env file not found, use environment variables or defaults
        }

        SpringApplication.run(AiSaleBackendApplication.class, args);
        System.out.println("AiSale Backend is running...");
    }
}