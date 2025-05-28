package com.selbuy.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.selbuy.dto.RegistrationDto;
import com.selbuy.service.RegistrationService;

@Controller
@RequestMapping("/auth")
public class RegistrationController {

    private final RegistrationService registrationService;
    private static final Logger logger = LoggerFactory.getLogger(RegistrationController.class);

    // Явный конструктор с внедрением зависимостей
    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrationDto", new RegistrationDto());
        return "auth/register";
    }
   @PostMapping("/register")
   public String registerUser(
           @Valid @ModelAttribute("registrationDto") RegistrationDto registrationDto,
           BindingResult bindingResult,
           RedirectAttributes redirectAttributes) {

       if (bindingResult.hasErrors()) {
           logger.warn("Ошибка валидации для пользователя: {}", registrationDto.getUsername());
           return "auth/register";
       }
       logger.info("Регистрация пользователя: {}", registrationDto.getUsername());

       try {
           registrationService.registerUser(registrationDto);
           redirectAttributes.addFlashAttribute("success",
                   "Регистрация успешна! Пожалуйста, проверьте ваш email для подтверждения.");
           logger.info("Пользователь {} успешно зарегистрирован", registrationDto.getUsername());

           return "auth/confirmation";
       } catch (Exception e) {
           logger.error("Ошибка регистрации пользователя: {}", registrationDto.getUsername(), e);
           bindingResult.reject("registration.error", e.getMessage());
           return "auth/register";
       }
   }
    @GetMapping("/confirmation")
    public String showConfirmationPage() {
        return "auth/confirmation";
    }

    @PostMapping("/verify")
    public String verifyToken(@RequestParam("token") String token,
                              RedirectAttributes redirectAttributes) {
        try {
            boolean verified = registrationService.verifyUser(token);
            if (verified) {
                redirectAttributes.addFlashAttribute("success",
                        "Ваш email успешно подтвержден! Теперь вы можете войти в систему.");
                return "redirect:/auth/login";
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/auth/confirmation";
    }

}