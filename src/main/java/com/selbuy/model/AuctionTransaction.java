// AuctionTransaction.java
package com.selbuy.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "auction_transactions")
@Data
public class AuctionTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long lotId;

    @Column(nullable = false)
    private String lotName;

    @Column(columnDefinition = "TEXT")
    private String lotDescription;

    @Column(nullable = false)
    private Double finalPrice;

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private User winner;

    @Column(name = "winner_id", insertable = false, updatable = false)
    private Long winnerId;

    @Column(nullable = false)
    private LocalDateTime transactionTime;

    @Column(nullable = false)
    private boolean paymentCompleted;
}