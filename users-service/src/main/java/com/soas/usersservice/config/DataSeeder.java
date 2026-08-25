package com.soas.usersservice.config;

import com.soas.library.dto.Role;
import com.soas.usersservice.entity.User;
import com.soas.usersservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository repository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public DataSeeder(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        repository.save(new User("owner@soas.rs", encoder.encode("owner123"), Role.OWNER));
        repository.save(new User("admin@soas.rs", encoder.encode("admin123"), Role.ADMIN));
        repository.save(new User("marko@soas.rs", encoder.encode("marko123"), Role.USER));
        repository.save(new User("ana@soas.rs", encoder.encode("ana123"), Role.USER));
        log.info("Ubaceni pocetni korisnici: {} zapisa", repository.count());
    }
}
