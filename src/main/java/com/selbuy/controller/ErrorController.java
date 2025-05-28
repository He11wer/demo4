package com.selbuy.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;

@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        // Получаем статус ошибки
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        Exception exception = (Exception) request.getAttribute("javax.servlet.error.exception");

        // Устанавливаем сообщения по умолчанию
        String errorMessage = "Произошла непредвиденная ошибка";
        int responseStatus = statusCode != null ? statusCode : HttpStatus.INTERNAL_SERVER_ERROR.value();

        // Определяем сообщение в зависимости от типа ошибки
        if (exception != null) {
            errorMessage = exception.getMessage();
        } else if (statusCode != null) {
            switch (statusCode) {
                case 404:
                    errorMessage = "Страница не найдена";
                    break;
                case 403:
                    errorMessage = "Доступ запрещен";
                    break;
                case 500:
                    errorMessage = "Внутренняя ошибка сервера";
                    break;
            }
        }

        // Добавляем атрибуты в модель
        model.addAttribute("statusCode", statusCode != null ? statusCode : "");
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("timestamp", LocalDateTime.now());

        return "error";
    }

    public String getErrorPath() {
        return "/error";
    }
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access-denied";
    }
}