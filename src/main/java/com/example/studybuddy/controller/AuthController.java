package com.example.studybuddy.controller;


import com.example.studybuddy.repository.UserRepository;
import com.example.studybuddy.repository.dto.LoginRequestDTO;
import com.example.studybuddy.repository.dto.UserRequestDTO;
import com.example.studybuddy.repository.dto.UserResponseDTO;
import com.example.studybuddy.repository.entity.User;
import com.example.studybuddy.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.coyote.Response;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.swing.text.html.Option;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "https://localhost:5173", allowCredentials = "true")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, ModelMapper modelMapper, AuthenticationManager authenticationManager, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest, HttpServletRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmailOrUsername(),
                            loginRequest.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();


            HttpSession session = request.getSession();
            session.setAttribute("user", userDetails.getUsername());

            return ResponseEntity.ok(Map.of(
                    "message", "User logged in successfully",
                    "username", userDetails.getUsername()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if(session != null) {
            session.invalidate();
        }

        return ResponseEntity.ok("User logged out successfully");
    }

    @GetMapping("/current-user")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if(session == null || session.getAttribute("user") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No user is logged in");
        }

        String username = session.getAttribute("user").toString();
        return ResponseEntity.ok(Map.of("username", username));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRequestDTO userRequestDTO) {

        //System.out.println("✅ Request received at /auth/register: " + userRequestDTO);

        if(userRepository.findByEmail(userRequestDTO.getEmail()).isPresent() ||
                userRepository.findByUsername(userRequestDTO.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Email or username already in use");
        }

        List<String> errors = new ArrayList<>();

        if(userRequestDTO.getPassword().length() < 8) {
            errors.add("Password must be at least 9 characters long");
        }
        if(!userRequestDTO.getPassword().matches(".*[A-Z].*")) {
            errors.add("Password must contain at least one uppercase letter");
        }
        if(!userRequestDTO.getPassword().matches(".*\\d.*")) {
            errors.add("Password must contain at least one digit");
        }
        if(!userRequestDTO.getPassword().matches(".*[@$!%*?&].*")) {
            errors.add("Password must contain at least one special character");
        }

        if(!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", errors.get(0)));
        }

        User newUser = new User();
        newUser.setUsername(userRequestDTO.getUsername());
        newUser.setFullName(userRequestDTO.getFullName());
        newUser.setEmail(userRequestDTO.getEmail());
        newUser.setPassword(passwordEncoder.encode(userRequestDTO.getPassword()));

        userRepository.save(newUser);
        return ResponseEntity.ok("User registered successfully");

    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        Optional<User> userOptional = userRepository.findByEmail(email);

        if(userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Email not found");
        }

        User user = userOptional.get();
        String verificationCode = String.format("%06d", new Random().nextInt(999999));

        user.setResetToken(verificationCode);
        user.setResetTokenExpiration(LocalDateTime.now().plusMinutes(10));

        userRepository.save(user);

        emailService.sendVerificationCode(user.getEmail(), verificationCode);

        System.out.println("Email sent successfully!");

        return ResponseEntity.ok(Map.of("message", "Verification code sent to email"));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        Optional<User> userOptional = userRepository.findByEmail(email);

        if(userOptional.isEmpty() || !userOptional.get().getResetToken().equals(code)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Invalid verification code"));
        }

        User user = userOptional.get();

        if (user.getResetToken() == null || !user.getResetToken().equals(code)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Invalid verification code"));
        }

        if (user.getResetTokenExpiration().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Verification code expired"));
        }

        return ResponseEntity.ok(Map.of("message", "Code verified successfully"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String newPassword = request.get("newPassword");

        Optional<User> userOptional = userRepository.findByEmail(email);

        if(userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        User user = userOptional.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiration(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }
}
