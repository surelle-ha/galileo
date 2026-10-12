package com.galileo.api.security;

import com.galileo.api.common.api.ApiResponse;
import com.galileo.api.common.exception.ResourceNotFoundException;
import com.galileo.api.user.UserRepository;
import com.galileo.api.user.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Issue a JWT and inspect the authenticated user")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    @Operation(summary = "Exchange email and password for a bearer token")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.of("Authentication successful", authenticationService.login(request));
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    @Operation(summary = "Return the current user's profile")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        var user = userRepository.findByEmailIgnoreCase(jwt.getSubject())
                .orElseThrow(() -> new ResourceNotFoundException("User", jwt.getSubject()));
        return ApiResponse.of("Current user retrieved", UserResponse.from(user));
    }
}
