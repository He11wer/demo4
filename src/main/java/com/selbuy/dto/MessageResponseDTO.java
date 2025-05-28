package com.selbuy.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MessageResponseDTO {
    private Long id;
    private Long senderId;
    private String senderName;
    private String message;
    private LocalDateTime createdAt;
}