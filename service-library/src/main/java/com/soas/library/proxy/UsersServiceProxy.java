package com.soas.library.proxy;

import com.soas.library.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "users-service")
public interface UsersServiceProxy {
    @GetMapping("/internal/users/{email}")
    UserDto findByEmail(@PathVariable("email") String email);
}
