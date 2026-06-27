package com.techora.auth;

import com.techora.common.application.dto.response.ResponseDto;
import com.techora.common.application.service.ResponseFactory;
import com.techora.auth.dto.request.LoginRequest;
import com.techora.auth.dto.request.RegisterRequest;
import com.techora.user.UserService;
import com.techora.common.infra.service.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;
    private final ResponseFactory responseFactory;

    @PostMapping("/register")
    public ResponseDto register(@Valid @RequestBody RegisterRequest request) {
        return responseFactory.success(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseDto login(@Valid @RequestBody LoginRequest request) {
        return responseFactory.success(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseDto me(@AuthenticationPrincipal UserPrincipal principal) {
        return responseFactory.success(userService.getCurrentUser(principal.getUserId()));
    }
}

