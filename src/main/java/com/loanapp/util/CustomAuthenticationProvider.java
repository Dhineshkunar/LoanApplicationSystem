package com.loanapp.util;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final CustomUserDetailsService userDetailsService;

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {

        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        UserDetails user = userDetailsService.loadUserByUsername(username);
        DesPasswordEncoder matcher = new DesPasswordEncoder();

        if (!matcher.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        return new UsernamePasswordAuthenticationToken(
                user,
                password,
                user.getAuthorities()
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Component
    public static class DesPasswordEncoder implements PasswordEncoder {

        @Override
        public String encode(CharSequence rawPassword) {
            try {
                return new StringEncrypter("DES").encrypt(rawPassword.toString());
            } catch (StringEncrypter.EncryptionException e) {
                throw new IllegalStateException("Password encoding failed", e);
            }
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {

            if (rawPassword == null || encodedPassword == null || encodedPassword.isBlank()) {
                return false;
            }

            try {
                String decrypted = new StringEncrypter("DES").decrypt(encodedPassword);
                String encryptraw = new StringEncrypter("DES").encrypt(rawPassword.toString());

                return decrypted.equals(encryptraw);
            } catch (StringEncrypter.EncryptionException e) {
                return false;
            }
        }
    }
}