package com.loanapp.service.impl;



import com.loanapp.dto.LoginRequest;
import com.loanapp.dto.LoginResponse;
import com.loanapp.entity.UserAccount;
import com.loanapp.repository.UserAccountRepository;
import com.loanapp.service.AuthService;
import com.loanapp.util.JwtService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;


@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LogManager.getLogger(AuthServiceImpl.class);
    public static final String ALPHABHETS_LOWER = "bdghjkmnpqrtvwxyz";
    public static final String ALPHABHETS_UPPER = ALPHABHETS_LOWER.toUpperCase()+"L";
    public static final String NUMBERS = "23456789";
    private static final int IMAGE_WIDTH = 160;
    private static final int IMAGE_HEIGHT = 40;
    private static final int TEXT_SIZE = 20;

    private final UserAccountRepository repository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    private static final Integer EXPIRE_MINS = 10;

    @Override
    public LoginResponse login(LoginRequest request) {

        try {
            Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            if (auth.isAuthenticated()) {
                return successfulLogin(request.getUsername());
            }
        } catch (Exception e) {
            logger.error(e);
        }
        return unsuccessfulLogin();
    }

    public LoginResponse successfulLogin(String username) {

        List<UserAccount> uaccList = repository.findByUserName(username);
        UserAccount account = uaccList.isEmpty()?null:uaccList.get(0);
        String token =  jwtService.generateToken(username,account.getId());

        LoginResponse lr = new LoginResponse();
        lr.setMessage("Login Successful");
        lr.setToken(token);
        lr.setUsername(account.getUsername());
        lr.setUserId(account.getId());
        return lr;
    }
    public LoginResponse unsuccessfulLogin() {
        LoginResponse lr = new LoginResponse();
        lr.setMessage("Invalid Username or Password");
        return lr;
    }

}