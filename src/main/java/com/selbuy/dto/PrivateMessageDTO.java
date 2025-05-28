package com.selbuy.dto;

import lombok.Data;

@Data
public class PrivateMessageDTO {
    private Long recipientId;
    private String message;
}