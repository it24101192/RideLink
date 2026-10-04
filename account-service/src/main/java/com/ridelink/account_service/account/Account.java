package com.ridelink.account_service.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import jakarta.persistence.PrePersist;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, length = 16)
    private UUID userId;

    @NotBlank(message = "Username must not be blank")
    @Size(min = 3, message = "Username must be at least 3 characters long")
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be valid")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Password must not be blank")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @JsonIgnore
    private String password;

    @NotBlank(message = "Role must not be blank")
    @Pattern(
            regexp = "PASSENGER|RIDER|DRIVER|ADMIN",
            message = "Role must be PASSENGER, DRIVER, or ADMIN"
    )
    private String role;

    @NotBlank(message = "Status must not be blank")
    private String status;

    public Account() {
    }

    public Long getId() {
        return id;
    }

    public UUID getUserId() { return userId; }

    public void setUserId(UUID userId) { this.userId = userId; }

    @PrePersist
    void assignUserId() {
        if (userId == null) userId = UUID.randomUUID();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
