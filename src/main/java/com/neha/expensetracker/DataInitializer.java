package com.neha.expensetracker;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("neha").isEmpty()) {
            User user = new User(
                    null,
                    "neha",
                    passwordEncoder.encode("pass123")
            );
            userRepository.save(user);
            System.out.println("✅ Test user created: neha / pass123");
        }
    }
}



