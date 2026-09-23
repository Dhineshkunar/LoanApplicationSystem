package com.loanapp.service;

import com.loanapp.dto.LoginRequest;
import com.loanapp.dto.LoginResponse;

public interface AuthService {
    public LoginResponse login(LoginRequest request);

    }
