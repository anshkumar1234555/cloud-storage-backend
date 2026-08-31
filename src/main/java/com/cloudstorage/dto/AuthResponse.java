package com.cloudstorage.dto;

public class AuthResponse {

    private Long id;
    private String name;
    private String email;
    private String provider;
    private String role;

    public AuthResponse(Long id,
                        String name,
                        String email,
                        String provider,
                        String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.provider = provider;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getProvider() {
        return provider;
    }

    public String getRole() {
        return role;
    }
}