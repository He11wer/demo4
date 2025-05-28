package com.selbuy.controller;

import com.selbuy.model.User;
import com.selbuy.repository.UserRepository;
import com.selbuy.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.selbuy.service.RegistrationService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final RegistrationService registrationService; // Теперь будет правильно внедрен
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;


    @GetMapping("/login")
    public String showLoginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        try {
            logger.debug("Обработка страницы входа: error={}, logout={}", error, logout);

            if (error != null) {
                logger.warn("Неудачная попытка входа");
                model.addAttribute("error", "Неверное имя пользователя или пароль");
            }

            if (logout != null) {
                logger.info("Успешный выход из системы");
                model.addAttribute("message", "Вы успешно вышли из системы");
                return "userhome";
            }

            return "auth/login";

        } catch (Exception e) {
            logger.error("Системная ошибка при отображении страницы входа", e);
            model.addAttribute("error", "Произошла системная ошибка");
            return "auth/login";
        }
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email, Model model) {
        try {
            Optional<User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isEmpty()) {
                model.addAttribute("error", "Пользователь с таким email не найден");
                return "auth/forgot-password";
            }

            User user = userOptional.get();
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiry(LocalDateTime.now().plusHours(24));
            userRepository.save(user);

            String resetLink = "http://localhost:8080/auth/reset-password?token=" + token;
            emailService.sendPasswordResetEmail(user, resetLink);

            model.addAttribute("message", "Ссылка для сброса пароля отправлена на ваш email");
            return "auth/forgot-password";
        } catch (Exception e) {
            logger.error("Ошибка при обработке запроса на сброс пароля", e);
            model.addAttribute("error", "Произошла ошибка при обработке запроса");
            return "auth/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model) {
        Optional<User> userOptional = userRepository.findByResetToken(token);
        if (userOptional.isEmpty() || userOptional.get().getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            model.addAttribute("error", "Недействительная или просроченная ссылка");
            return "auth/reset-password";
        }
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
                                       @RequestParam("password") String password,
                                       Model model) {
        try {
            Optional<User> userOptional = userRepository.findByResetToken(token);
            if (userOptional.isEmpty() || userOptional.get().getResetTokenExpiry().isBefore(LocalDateTime.now())) {
                model.addAttribute("error", "Недействительная или просроченная ссылка");
                return "auth/reset-password";
            }

            User user = userOptional.get();
            user.setPassword(passwordEncoder.encode(password));
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);

            model.addAttribute("message", "Пароль успешно изменен. Теперь вы можете войти.");
            return "auth/login";
        } catch (Exception e) {
            logger.error("Ошибка при сбросе пароля", e);
            model.addAttribute("error", "Произошла ошибка при сбросе пароля");
            return "auth/reset-password";
        }
    }


}
