package com.apidependencyradar.web;

import com.apidependencyradar.application.RegistrationService;
import com.apidependencyradar.infrastructure.persistence.AppUserEntity;
import com.apidependencyradar.infrastructure.persistence.AppUserRepository;
import com.apidependencyradar.web.dto.RegisterRequest;
import com.apidependencyradar.web.dto.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final AppUserRepository userRepository;

    public AuthController(RegistrationService registrationService, AppUserRepository userRepository) {
        this.registrationService = registrationService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @Valid @RequestBody RegisterRequest request) {
        registrationService.register(request);
        return ResponseEntity.created(URI.create("/api/auth/register")).build();
    }

    @GetMapping("/me")
    public UserProfileResponse me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UsernameNotFoundException("Not authenticated");
        }
        AppUserEntity user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getCreatedAt());
    }
}
