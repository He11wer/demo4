// LotTracking.java
package com.selbuy.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "lot_tracking")
@Data
public class LotTracking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "lot_id", nullable = false)
    private AuctionLot lot;

    @Column(nullable = false)
    private boolean liked = false;

    @Column(nullable = false)
    private boolean subscribed = false;
}