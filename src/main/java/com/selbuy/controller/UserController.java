package com.selbuy.controller;

import com.selbuy.dto.AuctionLotDTO;
import com.selbuy.dto.ChatMessageDTO;
import com.selbuy.dto.LotRatingDTO;
import com.selbuy.dto.MessageResponseDTO;
import com.selbuy.dto.PrivateMessageDTO;
import com.selbuy.model.*;
import com.selbuy.repository.*;
import com.selbuy.service.AuctionLotService;
import com.selbuy.service.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;
    private final AuctionLotService auctionLotService;
    private final LotRatingRepository lotRatingRepository;
    private final FileStorageService fileStorageService;
    private final AuctionLotRepository auctionLotRepository;
    private final LotTrackingRepository lotTrackingRepository;
    private final PasswordEncoder passwordEncoder;
    private final PrivateMessageRepository privateMessageRepository;
    private final AuctionTransactionRepository auctionTransactionRepository;

    // В UserController.java измените метод userDashboard:

    @GetMapping("/userhome")
    public String userDashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        model.addAttribute("username", user.getUsername());
        model.addAttribute("currentUserId", user.getId());

        // Загрузка активных лотов
        List<AuctionLot> activeLots = auctionLotService.getActiveLots();
        model.addAttribute("lots", activeLots);

        // Загрузка завершенных лотов, где пользователь был победителем
        List<AuctionTransaction> wonLots = auctionTransactionRepository.findByWinnerId(user.getId());
        model.addAttribute("wonLots", wonLots);

        // Передаем все доступные категории для фильтра
        model.addAttribute("allCategories", auctionLotService.getAllCategories());

        return "user/userhome";
    }

    @GetMapping("/profile")
    public String userProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<AuctionTransaction> wonTransactions = auctionTransactionRepository.findByWinnerId(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("wonTransactions", wonTransactions);
        return "user/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String username,
            @RequestParam(required = false) String profileDescription,
            RedirectAttributes redirectAttributes) {

        try {
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Проверка на уникальность ника
            if (!user.getUsername().equals(username) &&
                    userRepository.existsByUsername(username)) {
                redirectAttributes.addFlashAttribute("error", "Этот ник уже занят");
                return "redirect:/user/profile";
            }

            user.setUsername(username);
            user.setProfileDescription(profileDescription);
            userRepository.save(user);

            redirectAttributes.addFlashAttribute("success", "Профиль успешно обновлен");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении профиля");
        }
        return "redirect:/user/profile";
    }

    @GetMapping("/sell")
    public String showSellForm(Model model) {
        model.addAttribute("auctionLot", new AuctionLotDTO());
        return "user/sell";
    }

    @GetMapping("/lot/{id}/chat/messages")
    @ResponseBody
    public List<ChatMessage> getChatMessages(@PathVariable Long id) {
        return auctionLotService.getLotChatMessages(id);
    }

    @PostMapping("/lot/{id}/bid")
    public String placeBid(
            @PathVariable Long id,
            @RequestParam Double bidAmount,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        try {
            AuctionLot lot = auctionLotRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Лот не найден"));

            // Проверка, что у пользователя достаточно средств
            if (bidAmount > currentUser.getBalance()) {
                throw new IllegalArgumentException("Недостаточно средств на балансе");
            }

            // Проверка времени аукциона
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(lot.getStartTime())) {
                throw new IllegalArgumentException("Аукцион еще не начался");
            }
            if (now.isAfter(lot.getEndTime())) {
                throw new IllegalArgumentException("Аукцион уже завершен");
            }

            // Проверка минимальной ставки
            double minBid = lot.getLastBet() != null ?
                    lot.getLastBet() + lot.getMinBet() :
                    lot.getStartPrice() + lot.getMinBet();

            if (bidAmount < minBid) {
                throw new IllegalArgumentException("Ставка должна быть не меньше " + minBid);
            }

            // Возвращаем средства предыдущему участнику (если это не текущий пользователь)
            if (lot.getLastBetUserId() != null && !lot.getLastBetUserId().equals(currentUser.getId())) {
                User previousBidder = userRepository.findById(lot.getLastBetUserId())
                        .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
                previousBidder.setBalance(previousBidder.getBalance() + lot.getLastBet());
                userRepository.save(previousBidder);
            }

            // Снимаем средства с текущего пользователя
            currentUser.setBalance(currentUser.getBalance() - bidAmount);
            userRepository.save(currentUser);

            // Обновляем ставку в лоте через сервис (теперь с уведомлением)
            auctionLotService.placeBid(id, bidAmount, currentUser.getId());

            redirectAttributes.addFlashAttribute("success", "Ставка успешно размещена!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/user/lot/" + id;
    }




    @GetMapping("/api/users/search")
    @ResponseBody
    public List<User> searchUsers(@RequestParam String query) {
        return userRepository.findByUsernameContainingIgnoreCase(query);
    }

    @PostMapping("/api/user/update-password")
    @ResponseBody
    public ResponseEntity<?> updatePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request
    ) {
        try {
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            user.setPassword(passwordEncoder.encode(request.get("newPassword")));
            userRepository.save(user);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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

        return "user/product";
    }


    @GetMapping("/lot/{id}/refresh")
    @ResponseBody
    public Map<String, Object> refreshLotData(@PathVariable Long id) {
        AuctionLot lot = auctionLotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Лот не найден"));

        List<ChatMessage> chatMessages = auctionLotService.getLotChatMessages(id);
        List<LotRating> ratings = lotRatingRepository.findByLotId(id);

        return Map.of(
                "lastBet", lot.getLastBet(),
                "startPrice", lot.getStartPrice(),
                "averageRating", lot.getAverageRating(),
                "startTime", lot.getStartTime(),
                "endTime", lot.getEndTime(),
                "chatMessages", chatMessages.stream().map(msg -> Map.of(
                        "user", Map.of("username", msg.getUser().getUsername()),
                        "message", msg.getMessage(),
                        "createdAt", msg.getCreatedAt()
                )).collect(Collectors.toList()),
                "ratings", ratings.stream().map(rating -> Map.of(
                        "user", Map.of("username", rating.getUser().getUsername()),
                        "rating", rating.getRating(),
                        "comment", rating.getComment(),
                        "createdAt", rating.getCreatedAt()
                )).collect(Collectors.toList())
        );
    }

    @GetMapping("/lot/{id}/ratings")
    @ResponseBody
    public List<LotRating> getRatings(@PathVariable Long id) {
        return lotRatingRepository.findByLotId(id);
    }



    @GetMapping("/lot/{id}/current-bid")
    @ResponseBody
    public Map<String, Object> getCurrentBid(@PathVariable Long id) {
        AuctionLot lot = auctionLotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Лот не найден"));

        return Map.of(
                "lastBet", lot.getLastBet(),
                "startPrice", lot.getStartPrice(),
                "startTime", lot.getStartTime(),
                "endTime", lot.getEndTime()
        );
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
            return "redirect:/user/userhome";
        }

        AuctionLot lot = optionalLot.get();

        // Проверяем, что текущий пользователь - продавец
        if (!lot.getSeller().getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error", "Вы не можете удалить этот лот");
            return "redirect:/user/userhome";
        }

        try {
            auctionLotService.deleteLot(id);
            redirectAttributes.addFlashAttribute("success", "Лот успешно удален");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении лота: " + e.getMessage());
        }

        return "redirect:/user/userhome";
    }
    @PostMapping("/lot/{id}/chat")
    public String addChatMessage(@PathVariable Long id,
                                 @ModelAttribute("newMessage") @Valid ChatMessageDTO chatMessageDTO,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.newMessage", bindingResult);
            redirectAttributes.addFlashAttribute("newMessage", chatMessageDTO);
            return "redirect:/user/lot/" + id + "#chat";
        }

        try {
            User currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            auctionLotService.addChatMessage(id, currentUser.getId(), chatMessageDTO.getMessage());
            redirectAttributes.addFlashAttribute("chatSuccess", "Сообщение отправлено");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("chatError", "Ошибка при отправке сообщения: " + e.getMessage());
        }

        return "redirect:/user/lot/" + id + "#chat";
    }

    @PostMapping("/lot/{id}/rate")
    public String rateLot(@PathVariable Long id,
                          @ModelAttribute("newRating") LotRatingDTO ratingDTO,
                          @AuthenticationPrincipal UserDetails userDetails,
                          RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            auctionLotService.rateLot(id, currentUser.getId(), ratingDTO.getRating(), ratingDTO.getComment());
            redirectAttributes.addFlashAttribute("success", "Оценка сохранена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/user/lot/" + id;
    }



    @GetMapping("/lot/{id}/edit")
    public String showEditForm(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model) {
        AuctionLot lot = auctionLotService.getLotById(id)
                .orElseThrow(() -> new IllegalArgumentException("Лот не найден"));

        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!lot.getSeller().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Вы не можете редактировать этот лот");
        }

        AuctionLotDTO lotDTO = new AuctionLotDTO();
        lotDTO.setName(lot.getName());
        lotDTO.setDescription(lot.getDescription());
        lotDTO.setStartPrice(lot.getStartPrice());
        lotDTO.setMinBet(lot.getMinBet());
        lotDTO.setStartTime(lot.getStartTime());
        lotDTO.setEndTime(lot.getEndTime());
        lotDTO.setCategories(lot.getCategories());

        model.addAttribute("auctionLot", lotDTO);
        model.addAttribute("lotId", id);
        model.addAttribute("allCategories", auctionLotService.getAllCategories());
        return "user/edit-lot";
    }

    @PostMapping("/lot/{id}/edit")
    public String handleEditForm(@PathVariable Long id,
                                 @ModelAttribute("auctionLot") @Valid AuctionLotDTO auctionLotDTO,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 Model model) {
        try {
            AuctionLot lot = auctionLotService.getLotById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Лот не найден"));

            User currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            if (!lot.getSeller().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Вы не можете редактировать этот лот");
            }

            if (bindingResult.hasErrors()) {
                model.addAttribute("lotId", id);
                return "user/edit-lot";
            }

            // Обновляем данные лота
            lot.setName(auctionLotDTO.getName());
            lot.setDescription(auctionLotDTO.getDescription());
            lot.setStartPrice(auctionLotDTO.getStartPrice());
            lot.setMinBet(auctionLotDTO.getMinBet());
            lot.setStartTime(auctionLotDTO.getStartTime());
            lot.setEndTime(auctionLotDTO.getEndTime());
            lot.setCategories(auctionLotDTO.getCategories());

            if (auctionLotDTO.getImage() != null && !auctionLotDTO.getImage().isEmpty()) {
                String imageUrl = fileStorageService.storeFile(auctionLotDTO.getImage());
                lot.setImageUrl(imageUrl);
            }

            auctionLotRepository.save(lot);
            return "redirect:/user/lot/" + id + "?success";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка при обновлении лота: " + e.getMessage());
            model.addAttribute("lotId", id);
            return "user/edit-lot";
        }
    }


    @PostMapping("/sell")
    public String handleSellForm(
            @ModelAttribute("auctionLot") @Valid AuctionLotDTO auctionLotDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        // Проверка, что дата окончания позже даты начала
        if (auctionLotDTO.getEndTime() != null && auctionLotDTO.getStartTime() != null
                && auctionLotDTO.getEndTime().isBefore(auctionLotDTO.getStartTime())) {
            bindingResult.rejectValue("endTime", "error.auctionLot",
                    "Дата окончания должна быть позже даты начала");
        }

        if (bindingResult.hasErrors()) {
            return "user/sell";
        }

        try {
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            auctionLotService.createAuctionLot(auctionLotDTO, user);
            return "redirect:/user/userhome?success";
        } catch (IOException e) {
            model.addAttribute("error", "Ошибка при загрузке изображения: " + e.getMessage());
            return "user/sell";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка при создании лота: " + e.getMessage());
            return "user/sell";
        }
    }

    @GetMapping("/tracking")
    public String viewTracking(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<LotTracking> trackedLots = lotTrackingRepository.findByUserIdAndLikedTrue(user.getId());
        model.addAttribute("trackedLots", trackedLots);
        return "user/tracking";
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
        return "user/messages";
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




    @PostMapping("/lot/{id}/track")
    public String toggleTracking(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean subscribe,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        AuctionLot lot = auctionLotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lot not found"));

        Optional<LotTracking> existingTracking = lotTrackingRepository.findByUserIdAndLotId(user.getId(), lot.getId());

        LotTracking tracking;
        if (existingTracking.isPresent()) {
            tracking = existingTracking.get();
            tracking.setLiked(!tracking.isLiked());
            if (subscribe != null) {
                tracking.setSubscribed(subscribe);
            }
        } else {
            tracking = new LotTracking();
            tracking.setUser(user);
            tracking.setLot(lot);
            tracking.setLiked(true);
            if (subscribe != null) {
                tracking.setSubscribed(subscribe);
            }
        }

        lotTrackingRepository.save(tracking);

        return "redirect:/user/lot/" + id;
    }


}