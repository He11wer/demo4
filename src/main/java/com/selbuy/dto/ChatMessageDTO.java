package com.selbuy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatMessageDTO {
    private Long userId;

    @NotBlank(message = "Сообщение не может быть пустым")
    @Size(max = 500, message = "Сообщение слишком длинное")
    private String message;
}