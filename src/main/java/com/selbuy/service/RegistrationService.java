package com.selbuy.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.selbuy.dto.RegistrationDto;
import com.selbuy.model.Role;
import com.selbuy.model.User;
import com.selbuy.repository.RoleRepository;
import com.selbuy.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.UUID;

/*
@Service
public class RegistrationService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    public RegistrationService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void registerUser(RegistrationDto registrationDto) {
        logger.debug("Начинаем регистрацию пользователя: {}", registrationDto.getUsername());

        try {
            // 1. Проверка существования пользователя
            if (userRepository.existsByUsername(registrationDto.getUsername())) {
                throw new IllegalArgumentException("Имя пользователя уже занято");
            }
            if (userRepository.existsByEmail(registrationDto.getEmail())) {
                throw new IllegalArgumentException("Email уже используется");
            }

            // 2. Создание нового пользователя
            User user = new User();
            user.setUsername(registrationDto.getUsername());
            user.setEmail(registrationDto.getEmail());
            user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
            user.setEnabled(true);
            user.setEmailVerified(false);

            // 3. Получение или создание роли USER
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> {
                        Role newRole = new Role("ROLE_USER");
                        return roleRepository.save(newRole);
                    });

            // 4. Назначение роли
            user.getRoles().add(userRole);

            // 5. Сохранение с явным flush
            User savedUser = userRepository.saveAndFlush(user);

            logger.info("Пользователь {} успешно зарегистрирован", registrationDto.getUsername());
        } catch (Exception e) {
            logger.error("Ошибка при регистрации пользователя: {}", registrationDto.getUsername(), e);
        }
    }
}*/
@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    // Время жизни токена (24 часа)
    private static final int TOKEN_EXPIRATION_HOURS = 24;

    @Transactional
    public void registerUser(RegistrationDto registrationDto) {
        logger.debug("Начинаем регистрацию пользователя: {}", registrationDto.getUsername());

        // 1. Проверка существования пользователя
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new IllegalArgumentException("Имя пользователя уже занято");
        }
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new IllegalArgumentException("Email уже используется");
        }

        // 2. Создание нового пользователя
        User user = new User();
        user.setUsername(registrationDto.getUsername());
        user.setEmail(registrationDto.getEmail());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setEnabled(false); // Пользователь не активен до подтверждения email
        user.setEmailVerified(false);

        // Генерация токена подтверждения
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setTokenExpiry(LocalDateTime.now().plusHours(TOKEN_EXPIRATION_HOURS));

        // 3. Получение или создание роли USER
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role newRole = new Role("ROLE_USER");
                    return roleRepository.save(newRole);
                });

        // 4. Назначение роли
        user.getRoles().add(userRole);

        // 5. Сохранение пользователя
        User savedUser = userRepository.saveAndFlush(user);

        // 6. Отправка письма с подтверждением
        emailService.sendVerificationEmail(savedUser, token);

        logger.info("Пользователь {} успешно зарегистрирован. Ожидается подтверждение email.",
                registrationDto.getUsername());
    }

    @Transactional
    public boolean verifyUser(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Неверный токен подтверждения"));

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("Email уже подтвержден");
        }

        if (user.getTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Срок действия токена истек");
        }
        logger.info("Попытка подтверждения email с токеном: {}", token);
        user.setEmailVerified(true);
        user.setEnabled(true);
        user.setVerificationToken(null);
        user.setTokenExpiry(null);

        userRepository.save(user);
        return true;
    }
}