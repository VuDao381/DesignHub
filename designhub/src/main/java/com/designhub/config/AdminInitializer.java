package com.designhub.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.designhub.entity.Role;
import com.designhub.entity.User;
import com.designhub.repository.UserRepository;

@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:Admin@123456}")
    private String adminPassword;

    @Value("${app.admin.email:admin@designhub.com}")
    private String adminEmail;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByRole(Role.ADMIN)) {
            log.info("Chưa có tài khoản ADMIN trong hệ thống. Đang khởi tạo ADMIN mặc định...");

            User admin = new User();
            admin.setUsername(adminUsername.trim());
            admin.setEmail(adminEmail.trim());
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setFullName("System Administrator");
            admin.setPhone("0123456789");
            admin.setRole(Role.ADMIN);

            userRepository.save(admin);
            log.info("Khởi tạo tài khoản ADMIN thành công: {}", adminUsername);
        }
    }
}
