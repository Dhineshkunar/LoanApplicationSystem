package com.loanapp.util;



import com.loanapp.entity.UserAccount;
import com.loanapp.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        List<UserAccount> user = repository.findByUserName(username);

        if (user.isEmpty()) {
            throw new UsernameNotFoundException("Invalid Username");
        }

        return User.builder()
                .username(user.get(0).getUsername())
                .password(user.get(0).getPassword())
                .roles("USER")
                .build();
    }
}