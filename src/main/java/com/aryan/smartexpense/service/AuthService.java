package com.aryan.smartexpense.service;

import com.aryan.smartexpense.model.User;
import com.aryan.smartexpense.repository.UserRepository;
import com.aryan.smartexpense.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public Map<String, Object> register(String email, String password, String name, String phoneNumber) {
        email = normalizeEmail(email);
        phoneNumber = normalizePhone(phoneNumber);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        if (phoneNumber != null && !phoneNumber.isBlank() && userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("Phone number already registered");
        }

        String encodedPassword = passwordEncoder.encode(password);
        User user;
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            user = new User(email, encodedPassword, name, phoneNumber);
        } else {
            user = new User(email, encodedPassword, name);
        }
        user = userRepository.save(user);

        String token = jwtUtil.generateToken(email, user.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("userId", user.getId());
        response.put("email", user.getEmail());
        response.put("name", user.getName());
        response.put("phoneNumber", user.getPhoneNumber());
        return response;
    }

    public Map<String, Object> login(String email, String password) {
        email = normalizeEmail(email);
        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(email);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        User user = userOptional.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(email, user.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("userId", user.getId());
        response.put("email", user.getEmail());
        response.put("name", user.getName());
        return response;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String normalizePhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) return phoneNumber;
        String normalized = phoneNumber.replaceAll("[^0-9]", "");
        return normalized.isEmpty() ? null : normalized;
    }
}
