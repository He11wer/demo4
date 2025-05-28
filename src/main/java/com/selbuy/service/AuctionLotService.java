package com.selbuy.service;

import com.selbuy.dto.AuctionLotDTO;
import com.selbuy.model.*;
import com.selbuy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuctionLotService {

    private final AuctionLotRepository auctionLotRepository;
    private final FileStorageService fileStorageService;
    private final ChatMessageRepository chatMessageRepository;
    private final LotRatingRepository lotRatingRepository;
    private final UserRepository userRepository;
    private final LotTrackingRepository lotTrackingRepository;
    private final AuctionTransactionRepository auctionTransactionRepository;
    private final EmailService emailService;


    @Scheduled(fixedRate = 5000)
    @Transactional
    public void processCompletedAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<AuctionLot> completedLots = auctionLotRepository.findByEndTimeBeforeAndLastBetIsNotNull(now);

        for (AuctionLot lot : completedLots) {
            // Создаем транзакцию
            AuctionTransaction transaction = new AuctionTransaction();
            transaction.setLotId(lot.getId());
            transaction.setLotName(lot.getName());
            transaction.setLotDescription(lot.getDescription());
            transaction.setFinalPrice(lot.getLastBet());
            transaction.setSeller(lot.getSeller());

            if (lot.getLastBetUserId() != null) {
                User winner = userRepository.findById(lot.getLastBetUserId())
                        .orElseThrow(() -> new IllegalArgumentException("Winner not found"));
                transaction.setWinner(winner);

                // Отправляем уведомление победителю
                emailService.sendAuctionEndedNotification(winner, lot, true);

                // Отправляем уведомление продавцу
                emailService.sendAuctionEndedNotification(winner, lot, false);

                // Переводим деньги продавцу
                User seller = lot.getSeller();
                seller.setBalance(seller.getBalance() + lot.getLastBet());
                userRepository.save(seller);

                transaction.setPaymentCompleted(true);
            } else {
                transaction.setPaymentCompleted(false);
            }

            transaction.setTransactionTime(now);
            auctionTransactionRepository.save(transaction);

            // Удаляем лот и связанные данные
            deleteLot(lot.getId());
        }
    }



    @Transactional
    public AuctionLot createAuctionLot(AuctionLotDTO lotDTO, User seller) throws IOException {
        AuctionLot lot = new AuctionLot();
        lot.setSeller(seller);
        lot.setName(lotDTO.getName());
        lot.setStartTime(lotDTO.getStartTime());
        lot.setEndTime(lotDTO.getEndTime());
        lot.setDescription(lotDTO.getDescription());
        lot.setStartPrice(lotDTO.getStartPrice());
        lot.setMinBet(lotDTO.getMinBet());
        lot.setCategories(lotDTO.getCategories());

        if (lotDTO.getImage() != null && !lotDTO.getImage().isEmpty()) {
            String imageUrl = fileStorageService.storeFile(lotDTO.getImage());
            lot.setImageUrl(imageUrl);
        }



        return auctionLotRepository.save(lot);
    }

    @Transactional
    public void placeBid(Long lotId, Double bidAmount, Long userId) {
        AuctionLot lot = auctionLotRepository.findById(lotId)
                .orElseThrow(() -> new IllegalArgumentException("Лот не найден"));

        // Если есть предыдущий участник и это не текущий пользователь - отправляем уведомление
        if (lot.getLastBetUserId() != null && !lot.getLastBetUserId().equals(userId)) {
            userRepository.findById(lot.getLastBetUserId()).ifPresent(previousBidder -> {
                emailService.sendOutbidNotification(previousBidder, lot);
            });
        }

        // Обновляем ставку
        lot.setLastBet(bidAmount);
        lot.setLastBetUserId(userId);
        auctionLotRepository.save(lot);
    }
    public Optional<AuctionLot> getLotById(Long id) {
        return auctionLotRepository.findById(id);
    }

    public List<AuctionLot> getAllLots() {
        return auctionLotRepository.findAll();
    }

    @Transactional
    public void deleteLot(Long id) {
        AuctionLot lot = auctionLotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Лот не найден"));

        // Удаляем связанные записи
        lotTrackingRepository.deleteByLotId(id);
        chatMessageRepository.deleteByLotId(id);
        lotRatingRepository.deleteByLotId(id);


        // Удаляем сам лот
        auctionLotRepository.deleteById(id);
    }



    public List<ChatMessage> getLotChatMessages(Long lotId) {
        return chatMessageRepository.findByLotIdOrderByCreatedAtAsc(lotId);
    }

    @Transactional
    public ChatMessage addChatMessage(Long lotId, Long userId, String message) {
        AuctionLot lot = auctionLotRepository.findById(lotId)
                .orElseThrow(() -> new IllegalArgumentException("Лот не найден"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setLot(lot);
        chatMessage.setUser(user);
        chatMessage.setMessage(message);

        return chatMessageRepository.save(chatMessage);
    }




    @Transactional
    public void rateLot(Long lotId, Long userId, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Рейтинг должен быть от 1 до 5");
        }

        AuctionLot lot = auctionLotRepository.findById(lotId)
                .orElseThrow(() -> new IllegalArgumentException("Лот не найден"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        Optional<LotRating> existingRating = lotRatingRepository.findByLotIdAndUserId(lotId, userId);

        LotRating lotRating;
        if (existingRating.isPresent()) {
            lotRating = existingRating.get();
        } else {
            lotRating = new LotRating();
            lotRating.setLot(lot);
            lotRating.setUser(user);
        }

        lotRating.setRating(rating);
        lotRating.setComment(comment);
        lotRatingRepository.save(lotRating);

        // Обновляем средний рейтинг лота
        updateLotAverageRating(lotId);
    }

    private void updateLotAverageRating(Long lotId) {
        List<LotRating> ratings = lotRatingRepository.findByLotId(lotId);
        if (!ratings.isEmpty()) {
            double average = ratings.stream()
                    .mapToInt(LotRating::getRating)
                    .average()
                    .orElse(0.0);
            AuctionLot lot = auctionLotRepository.findById(lotId).orElseThrow();
            lot.setAverageRating(average);
            auctionLotRepository.save(lot);
        }
    }
    @Transactional
    public void updateLot(AuctionLot lot) {
        // Проверяем, что лот существует
        if (!auctionLotRepository.existsById(lot.getId())) {
            throw new IllegalArgumentException("Лот не найден");
        }

        // Проверяем даты
        if (lot.getEndTime().isBefore(lot.getStartTime())) {
            throw new IllegalArgumentException("Дата окончания должна быть позже даты начала");
        }

        // Сохраняем обновленный лот
        auctionLotRepository.save(lot);
    }


    public List<String> getAllCategories() {
        // Здесь можно вернуть список всех возможных категорий
        return List.of("Электроника", "Одежда", "Мебель", "Книги", "Спорт");
    }

    public List<AuctionLot> getActiveLots() {
        LocalDateTime now = LocalDateTime.now();
        return auctionLotRepository.findByEndTimeAfter(now);
    }
}