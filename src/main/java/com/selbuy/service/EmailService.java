package com.selbuy.service;

import com.selbuy.model.AuctionLot;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.selbuy.model.User;

@Service
@RequiredArgsConstructor
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final Environment env;

    public void sendVerificationEmail(User user, String token) {
        try {
            String subject = "Подтверждение регистрации";
            String message = "Ваш код подтверждения: " + token +
                    "\nВведите этот код на странице подтверждения для завершения регистрации.";

            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo(user.getEmail());
            email.setSubject(subject);
            email.setText(message);
            email.setFrom(env.getProperty("spring.mail.username"));

            mailSender.send(email);
            logger.info("Письмо с кодом подтверждения отправлено на: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Ошибка при отправке письма с подтверждением", e);
            throw new RuntimeException("Не удалось отправить письмо с подтверждением");
        }
    }
    // EmailService.java
    public void sendOutbidNotification(User user, AuctionLot lot) {
        try {
            String subject = "Ваша ставка перебита";
            String message = String.format(
                    "Ваша ставка на лот \"%s\" была перебита.",
                    lot.getName(),
                    lot.getLastBet()
            );

            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo(user.getEmail());
            email.setSubject(subject);
            email.setText(message);
            email.setFrom(env.getProperty("spring.mail.username"));

            mailSender.send(email);
            logger.info("Уведомление о перебитой ставке отправлено на: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Ошибка при отправке уведомления о перебитой ставке", e);
        }
    }

    public void sendAuctionEndedNotification(User winner, AuctionLot lot, boolean isWinner) {
        try {
            String subject = isWinner ? "Вы выиграли аукцион!" : "Аукцион завершен";
            String message;

            if (isWinner) {
                message = String.format(
                        "Поздравляем! Вы выиграли лот \"%s\" за %.2f. Свяжитесь с продавцом для завершения сделки.",
                        lot.getName(),
                        lot.getLastBet()
                );
            } else {
                message = String.format(
                        "Аукцион на лот \"%s\" завершен. Победитель: %s (%.2f)",
                        lot.getName(),
                        winner != null ? winner.getUsername() : "нет победителя",
                        lot.getLastBet()
                );
            }

            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo(isWinner ? winner.getEmail() : lot.getSeller().getEmail());
            email.setSubject(subject);
            email.setText(message);
            email.setFrom(env.getProperty("spring.mail.username"));

            mailSender.send(email);
            logger.info("Уведомление о завершении аукциона отправлено на: {}",
                    isWinner ? winner.getEmail() : lot.getSeller().getEmail());
        } catch (Exception e) {
            logger.error("Ошибка при отправке уведомления о завершении аукциона", e);
        }
    }

    public void sendPasswordResetEmail(User user, String resetLink) {
        try {
            String subject = "Res password";
            String message = resetLink;


            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo(user.getEmail());
            email.setSubject(subject);
            email.setText(message);
            email.setFrom(env.getProperty("spring.mail.username"));

            logger.info("Попытка отправки письма на: {}", user.getEmail());
            mailSender.send(email); // Логирование перед отправкой
            logger.info("Письмо успешно отправлено!");
        } catch (MailAuthenticationException e) {
            logger.error("Ошибка аутентификации SMTP: {}", e.getMessage());
            throw new RuntimeException("Проверьте настройки почты (логин/пароль)");
        } catch (MailSendException e) {
            logger.error("Ошибка отправки письма: {}", e.getMessage());
            throw new RuntimeException("Ошибка SMTP-сервера");
        } catch (Exception e) {
            logger.error("Неизвестная ошибка: {}", e.getMessage(), e);
            throw new RuntimeException("Произошла ошибка при отправке письма");
        }
    }




}