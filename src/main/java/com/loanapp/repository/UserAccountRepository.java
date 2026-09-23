package com.loanapp.repository;

import com.loanapp.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAccountRepository extends JpaRepository<UserAccount,Long> {

    List<UserAccount> findByUserName(String username);
}
