package com.soas.usersservice.controller;

import com.soas.library.dto.AuthRequest;
import com.soas.library.dto.AuthResponse;
import com.soas.library.dto.UserDto;
import com.soas.usersservice.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
public class InternalUserController {
    private final UserService service;

    public InternalUserController(UserService service) {
        this.service = service;
    }

    @GetMapping("/users/{email}")
    public UserDto findByEmail(@PathVariable String email) {
        return service.findByEmailInternal(email);
    }

    @PostMapping("/users/authenticate")
    public AuthResponse authenticate(@RequestBody AuthRequest request) {
        UserDto user = service.authenticate(request.getEmail(), request.getPassword());
        return new AuthResponse(true, user.getEmail(), user.getRole());
    }
}
