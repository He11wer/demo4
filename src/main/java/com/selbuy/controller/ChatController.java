// ChatController.java
package com.selbuy.controller;

import com.selbuy.dto.ChatMessageDTO;
import com.selbuy.model.ChatMessage;
import com.selbuy.service.AuctionLotService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {
    private final AuctionLotService auctionLotService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(AuctionLotService auctionLotService, SimpMessagingTemplate messagingTemplate) {
        this.auctionLotService = auctionLotService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat/{lotId}")
    public void sendMessage(@DestinationVariable Long lotId, ChatMessageDTO message) {
        ChatMessage savedMessage = auctionLotService.addChatMessage(lotId, message.getUserId(), message.getMessage());
        messagingTemplate.convertAndSend("/topic/messages/" + lotId, savedMessage);
    }
}