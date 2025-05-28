package com.selbuy.repository;

import com.selbuy.model.AuctionLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionLotRepository extends JpaRepository<AuctionLot, Long> {
    List<AuctionLot> findBySellerId(Long sellerId);
    List<AuctionLot> findByEndTimeAfter(LocalDateTime now);
    Optional<AuctionLot> findById(Long id);
    List<AuctionLot> findAll(); //
    List<AuctionLot> findByEndTimeBeforeAndLastBetIsNotNull(LocalDateTime endTime);
}