package com.rental.rental_system.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String token;
    private String message;
}
