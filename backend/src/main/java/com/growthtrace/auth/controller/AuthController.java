package com.growthtrace.auth.controller;

import com.growthtrace.auth.dto.LoginRequest;
import com.growthtrace.auth.dto.LoginResponse;
import com.growthtrace.auth.dto.RegisterRequest;
import com.growthtrace.auth.dto.UserInfoVO;
import com.growthtrace.auth.service.AuthService;
import com.growthtrace.common.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return R.ok();
    }

    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    @GetMapping("/me")
    public R<UserInfoVO> me() {
        return R.ok(authService.currentUser());
    }
}
