package com.rental.rental_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class RentalSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(RentalSystemApplication.class, args);
    }
}