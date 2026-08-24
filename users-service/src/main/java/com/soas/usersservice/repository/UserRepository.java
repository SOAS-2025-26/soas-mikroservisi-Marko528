package com.soas.usersservice.repository;

import com.soas.library.dto.Role;
import com.soas.usersservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByRole(Role role);

    long countByRole(Role role);
}
