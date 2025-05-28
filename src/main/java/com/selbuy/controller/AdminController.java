package com.selbuy.controller;

import com.selbuy.dto.ChatMessageDTO;
import com.selbuy.dto.LotRatingDTO;
import com.selbuy.dto.MessageResponseDTO;
import com.selbuy.dto.PrivateMessageDTO;
import com.selbuy.model.*;
import com.selbuy.repository.*;
import com.selbuy.service.AuctionLotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserRepository userRepository;
    private final AuctionLotService auctionLotService;
    private final AuctionTransactionRepository transactionRepository;
    private final PrivateMessageRepository privateMessageRepository;
    private final RoleRepository roleRepository;
    private final LotRatingRepository lotRatingRepository;

    @GetMapping("/userhome")
    public String adminDashboard(Model model) {
        model.addAttribute("lots", auctionLotService.getAllLots());
        return "admin/userhome";
    }

    @GetMapping("/users")
    public String manageUsers(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "admin/users";
    }

    @PostMapping("/block/{userId}")
    public String blockUser(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN"))) {
            redirectAttributes.addFlashAttribute("error", "Cannot block admin user");
            return "redirect:/admin/users";
        }
        Role blockedRole = roleRepository.findByName("ROLE_BANNED")
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        user.getRoles().clear();
        user.getRoles().add(blockedRole);
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "User blocked successfully");
        return "redirect:/admin/users";
    }

    @PostMapping("/unblock/{userId}")
    public String unblockUser(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        user.getRoles().clear();
        user.getRoles().add(userRole);
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "User unblocked successfully");
        return "redirect:/admin/users";
    }

    @PostMapping("/add-funds/{userId}")
    public String addFunds(@PathVariable Long userId,
                           @RequestParam Double amount,
                           RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setBalance(user.getBalance() + amount);
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success", String.format("Added %.2f to user balance", amount));
        return "redirect:/admin/users";
    }

    @PostMapping("/remove-funds/{userId}")
    public String removeFunds(@PathVariable Long userId,
                              @RequestParam Double amount,
                              RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getBalance() < amount) {
            redirectAttributes.addFlashAttribute("error", "Insufficient funds");
        } else {
            user.setBalance(user.getBalance() - amount);
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("success", String.format("Removed %.2f from user balance", amount));
        }

        return "redirect:/admin/users";
    }

    @GetMapping("/transactions/{userId}")
    public String viewUserTransactions(@PathVariable Long userId, Model model) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<AuctionTransaction> asSeller = transactionRepository.findBySellerId(userId);
        List<AuctionTransaction> asBuyer = transactionRepository.findByWinnerId(userId);

        model.addAttribute("user", user);
        model.addAttribute("asSeller", asSeller);
        model.addAttribute("asBuyer", asBuyer);

        return "admin/transactions";
    }

    @GetMapping("/lot/{id}")
    public String viewLot(@PathVariable Long id,
                          @AuthenticationPrincipal UserDetails userDetails,
                          Model model) {
        Optional<AuctionLot> optionalLot = auctionLotService.getLotById(id);
        if (!optionalLot.isPresent()) {
            throw new IllegalArgumentException("Лот не найден");
        }

        AuctionLot lot = optionalLot.get();
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Получаем сообщения чата
        List<ChatMessage> chatMessages = auctionLotService.getLotChatMessages(id);

        // Получаем оценки
        List<LotRating> ratings = lotRatingRepository.findByLotId(id);
        Optional<LotRating> userRating = lotRatingRepository.findByLotIdAndUserId(id, currentUser.getId());

        model.addAttribute("bidAmount", 0.0);
        model.addAttribute("lot", lot);
        model.addAttribute("user", currentUser); // Добавляем объект пользователя
        model.addAttribute("isSeller", lot.getSeller().getId().equals(currentUser.getId()));
        model.addAttribute("chatMessages", chatMessages);
        model.addAttribute("ratings", ratings);
        model.addAttribute("userRating", userRating.orElse(null));
        model.addAttribute("newMessage", new ChatMessageDTO());
        model.addAttribute("newRating", new LotRatingDTO());

        return "admin/product";
    }


    @PostMapping("/lot/{id}/delete")
    public String deleteLot(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Optional<AuctionLot> optionalLot = auctionLotService.getLotById(id);
        if (!optionalLot.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Лот не найден");
            return "redirect:/admin/userhome";
        }



        try {
            auctionLotService.deleteLot(id);
            redirectAttributes.addFlashAttribute("success", "Лот успешно удален");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении лота: " + e.getMessage());
        }

        return "redirect:/admin/userhome";
    }

    @GetMapping("/profile")
    public String userProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        model.addAttribute("user", user);
        return "admin/profile";
    }

    @GetMapping("/messages")
    public String userMessages(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Получаем всех пользователей кроме текущего
        List<User> allUsers = userRepository.findAll()
                .stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .collect(Collectors.toList());

        model.addAttribute("allUsers", allUsers);
        model.addAttribute("currentUser", currentUser);
        return "admin/messages";
    }

    @GetMapping("/messages/chat/{userId}")
    @ResponseBody
    public List<MessageResponseDTO> getPrivateMessages(@PathVariable Long userId,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return privateMessageRepository.findConversation(currentUser.getId(), userId)
                .stream()
                .map(msg -> {
                    MessageResponseDTO dto = new MessageResponseDTO();
                    dto.setId(msg.getId());
                    dto.setSenderId(msg.getSender().getId());
                    dto.setSenderName(msg.getSender().getUsername());
                    dto.setMessage(msg.getMessage());
                    dto.setCreatedAt(msg.getCreatedAt());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/messages/send")
    @ResponseBody
    public ResponseEntity<?> sendPrivateMessage(@RequestBody PrivateMessageDTO messageDTO,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User sender = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            User recipient = userRepository.findById(messageDTO.getRecipientId())
                    .orElseThrow(() -> new UsernameNotFoundException("Recipient not found"));

            PrivateMessage message = new PrivateMessage();
            message.setSender(sender);
            message.setRecipient(recipient);
            message.setMessage(messageDTO.getMessage());
            message.setCreatedAt(LocalDateTime.now());

            privateMessageRepository.save(message);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при отправке сообщения: " + e.getMessage());
        }
    }


}