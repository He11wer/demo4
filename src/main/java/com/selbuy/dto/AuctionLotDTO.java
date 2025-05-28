package com.selbuy.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AuctionLotDTO {
    @NotBlank(message = "Название не может быть пустым")
    @Size(max = 100, message = "Название слишком длинное")
    private String name;

    @NotNull(message = "Укажите дату начала")
    @Future(message = "Дата начала должна быть в будущем")
    private LocalDateTime startTime;

    @NotNull(message = "Укажите дату окончания")
    @Future(message = "Дата окончания должна быть в будущем")
    private LocalDateTime endTime;

    @NotBlank(message = "Описание не может быть пустым")
    @Size(max = 1000, message = "Описание слишком длинное")
    private String description;

    @NotNull(message = "Укажите начальную цену")
    @Positive(message = "Цена должна быть положительной")
    private double startPrice;

    @NotNull(message = "Укажите минимальную ставку")
    @Positive(message = "Ставка должна быть положительной")
    private double minBet;

    @NotEmpty(message = "Выберите хотя бы одну категорию")
    private List<String> categories;

    @NotNull(message = "Загрузите изображение")
    private MultipartFile image;
}