package com.selbuy.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "auction_lots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuctionLot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seller_id", nullable = false)
    private User seller;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(nullable = false)
    private double startPrice;

    @Column(nullable = false)
    private double minBet;

    @Column(nullable = true)
    private Double lastBet;

    @Column(nullable = true)
    private Long lastBetUserId;

    @Column(nullable = false)
    private String imageUrl;

    @ElementCollection
    @NotEmpty(message = "Выберите хотя бы одну категорию")
    @CollectionTable(name = "lot_categories", joinColumns = @JoinColumn(name = "lot_id"))
    @Column(name = "category")
    private List<String> categories;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private Double averageRating;

    @OneToMany(mappedBy = "lot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> chatMessages;

    @OneToMany(mappedBy = "lot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LotRating> ratings;

    @OneToMany(mappedBy = "lot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LotTracking> trackings;

}