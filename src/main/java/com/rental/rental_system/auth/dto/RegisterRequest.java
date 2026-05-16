package com.rental.rental_system.auth.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class RegisterRequest {
    @NotBlank private String name;
    @Email @NotBlank private String email;
    @Size(min = 6) private String password;
    private String phone;
    @NotBlank private String role; // "ADMIN", "TENANT", "CARETAKER"
}
