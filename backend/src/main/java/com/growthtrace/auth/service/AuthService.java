package com.growthtrace.auth.service;

import com.growthtrace.auth.dto.LoginRequest;
import com.growthtrace.auth.dto.LoginResponse;
import com.growthtrace.auth.dto.RegisterRequest;
import com.growthtrace.auth.dto.UserInfoVO;

public interface AuthService {

    void register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    UserInfoVO currentUser();
}
