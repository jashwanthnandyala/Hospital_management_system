package com.Project.Hospital_Management_System.controller;

import com.Project.Hospital_Management_System.dto.JwtResponse;
import com.Project.Hospital_Management_System.dto.LoginRequest;
import com.Project.Hospital_Management_System.dto.SignupRequest;
import com.Project.Hospital_Management_System.entity.User;
import com.Project.Hospital_Management_System.repository.UserRepository;
import com.Project.Hospital_Management_System.security.jwt.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public authentication endpoints.
 *   POST /api/auth/signup  → register a new PATIENT (only role allowed via self-registration)
 *   POST /api/auth/signin  → login for any user (patient / doctor / admin)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    // ── SIGN IN ──────────────────────────────────────────────────────────
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority())
                .orElse("ROLE_PATIENT");

        return ResponseEntity.ok(new JwtResponse(jwt, userDetails.getUsername(), role));
    }

    // ── SIGN UP (patient only) ───────────────────────────────────────────
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signupRequest) {

        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username is already taken!"));
        }

        User user = new User();
        user.setUsername(signupRequest.getUsername());
        user.setEmail(signupRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(signupRequest.getPassword()));
        user.setRole("ROLE_PATIENT"); // only patients can self-register
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Patient registered successfully!"));
    }
}
