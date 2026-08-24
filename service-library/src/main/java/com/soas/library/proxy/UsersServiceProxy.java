package com.soas.library.proxy;

import com.soas.library.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign proxy ka users-service mikroservisu.
 * Koriste ga bank-account i crypto-wallet da provere da li korisnik postoji
 * i da li ima ulogu USER.
 */
@FeignClient(name = "users-service")
public interface UsersServiceProxy {

    /** Interni endpoint - vraca korisnika po email adresi, bez provere uloge pozivaoca. */
    @GetMapping("/internal/users/{email}")
    UserDto findByEmail(@PathVariable("email") String email);
}
