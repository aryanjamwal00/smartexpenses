package com.aryan.smartexpense.controller;

import com.aryan.smartexpense.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            String password = payload.get("password");
            String name = payload.get("name");
            String phoneNumber = payload.get("phoneNumber");

            if (email == null || email.isBlank() || password == null || password.isBlank() || name == null || name.isBlank()) {
                return ResponseEntity.badRequest().body("Email, password, and name are required");
            }

            Map<String, Object> result = authService.register(email, password, name, phoneNumber);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Registration failed: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            String password = payload.get("password");

            if (email == null || email.isBlank() || password == null || password.isBlank()) {
                return ResponseEntity.badRequest().body("Email and password are required");
            }

            Map<String, Object> result = authService.login(email, password);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Login failed: " + e.getMessage());
        }
    }
}
